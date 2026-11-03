package com.vaulta.sync

import android.util.Log
import com.vaulta.storage.blob.BlobStore
import com.vaulta.storage.index.VaultEntryDao
import com.vaulta.storage.index.toEntity
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.storage.index.SyncStatus
import com.vaulta.sync.drive.DriveSyncClient
import com.vaulta.vault.session.VaultSession
import com.vaulta.vault.sync.SyncProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngine @Inject constructor(
    private val driveClient: DriveSyncClient,
    private val vaultEntryDao: VaultEntryDao,
    private val blobStore: BlobStore,
    private val encryptedIndex: EncryptedIndex,
    private val vaultSession: VaultSession
) : SyncProvider {

    override val isAvailable: Boolean
        get() = driveClient.isSignedIn()

    override val providerName: String
        get() = "Google Drive"

    override suspend fun sync(): Result<Unit> = performSync()

    fun isSyncAvailable(): Boolean = driveClient.isSignedIn()

    suspend fun performSync(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!driveClient.isSignedIn()) {
            return@withContext Result.failure(IllegalStateException("Google Drive not signed in"))
        }

        if (!vaultSession.isUnlocked.value) {
            return@withContext Result.failure(IllegalStateException("Vault is locked"))
        }

        try {
            vaultSession.withMasterKey { masterKey ->
                // Step 0: Download remote index from Google Drive and merge remote changes
                Log.d(TAG, "Step 0: Checking and downloading remote index from Google Drive...")
                val remoteIndexBytes = driveClient.downloadIndex()
                val isFirstSync = (remoteIndexBytes == null || remoteIndexBytes.isEmpty())
                val remoteEntries = if (!isFirstSync) {
                    try {
                        encryptedIndex.parseFromEncryptedData(remoteIndexBytes!!, masterKey)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed decrypting remote index with active master key. Aborting sync immediately to prevent data loss.", e)
                        throw IllegalStateException("Failed decrypting remote index: ${e.message}", e)
                    }
                } else {
                    Log.d(TAG, "No remote index found on Google Drive, treating as initial sync (no remote deletions).")
                    emptyList()
                }

                val remoteMap = remoteEntries.associateBy { it.id }
                val localEntities = vaultEntryDao.getAllEntries()
                val localMap = localEntities.associateBy { it.id }

                // Track IDs locally marked for deletion before merge
                val locallyDeletedIds = localEntities
                    .filter { it.syncStatus == SyncStatus.DELETED.name }
                    .map { it.id }
                    .toSet()

                // Step 1: Process Remote Delta -> Merge into local Room DB & download missing blobs
                for (remoteEntry in remoteEntries) {
                    val localEntity = localMap[remoteEntry.id]

                    if (localEntity == null) {
                        // Entry exists remotely but not in local database
                        if (locallyDeletedIds.contains(remoteEntry.id)) {
                            // User on this device explicitly deleted it; sync deletion to remote
                            remoteEntry.blobName?.let { driveClient.deleteBlob(it) }
                        } else {
                            // New file added from another device -> Pull blob and insert locally
                            remoteEntry.blobName?.let { bName ->
                                val destBlob = blobStore.getBlobFile(bName)
                                if (!destBlob.exists()) {
                                    val downloaded = driveClient.downloadBlob(bName, destBlob)
                                    if (!downloaded) {
                                        Log.w(TAG, "Failed downloading remote blob $bName")
                                    }
                                }
                            }
                            vaultEntryDao.insertEntry(remoteEntry.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                        }
                    } else {
                        // Entry exists on both local and remote
                        when {
                            localEntity.syncStatus == SyncStatus.DELETED.name -> {
                                // Local deletion takes precedence; remove remote blob
                                remoteEntry.blobName?.let { driveClient.deleteBlob(it) }
                                vaultEntryDao.deleteEntry(localEntity)
                            }

                            localEntity.syncStatus == SyncStatus.PENDING.name &&
                                    localEntity.blobName != remoteEntry.blobName &&
                                    localEntity.modifiedAt != remoteEntry.modifiedAt -> {
                                // CONFLICT: Both devices modified the file simultaneously
                                val timestamp = System.currentTimeMillis()
                                val dotIndex = localEntity.name.lastIndexOf('.')
                                val conflictName = if (dotIndex > 0) {
                                    val base = localEntity.name.substring(0, dotIndex)
                                    val ext = localEntity.name.substring(dotIndex)
                                    "$base (conflict_$timestamp)$ext"
                                } else {
                                    "${localEntity.name} (conflict_$timestamp)"
                                }

                                // Preserve local modifications as a new conflict copy
                                val conflictCopy = localEntity.copy(
                                    id = UUID.randomUUID().toString(),
                                    name = conflictName,
                                    syncStatus = SyncStatus.PENDING.name
                                )
                                vaultEntryDao.insertEntry(conflictCopy)
                                conflictCopy.blobName?.let { bName ->
                                    val bFile = blobStore.getBlobFile(bName)
                                    if (bFile.exists()) driveClient.uploadBlob(bName, bFile)
                                }

                                // Download remote update and adopt it for the canonical entry
                                remoteEntry.blobName?.let { rBlob ->
                                    val destBlob = blobStore.getBlobFile(rBlob)
                                    if (!destBlob.exists()) {
                                        driveClient.downloadBlob(rBlob, destBlob)
                                    }
                                }
                                vaultEntryDao.insertEntry(remoteEntry.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                            }

                            localEntity.syncStatus == SyncStatus.SYNCED.name &&
                                    remoteEntry.modifiedAt > localEntity.modifiedAt -> {
                                // Remote is newer; update local copy
                                remoteEntry.blobName?.let { rBlob ->
                                    val destBlob = blobStore.getBlobFile(rBlob)
                                    if (!destBlob.exists()) {
                                        driveClient.downloadBlob(rBlob, destBlob)
                                    }
                                }
                                vaultEntryDao.updateEntry(remoteEntry.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                            }
                        }
                    }
                }

                // Step 2: Process Local Delta -> Remote
                val refreshedLocalEntities = vaultEntryDao.getAllEntries()
                for (localEntity in refreshedLocalEntities) {
                    if (localEntity.syncStatus == SyncStatus.DELETED.name) {
                        localEntity.blobName?.let { bName ->
                            driveClient.deleteBlob(bName)
                            blobStore.deleteBlob(bName)
                        }
                        vaultEntryDao.deleteEntry(localEntity)
                    } else if (!remoteMap.containsKey(localEntity.id)) {
                        // Local entry missing from remote index
                        if (!isFirstSync && localEntity.syncStatus == SyncStatus.SYNCED.name) {
                            // Only propagate remote deletion if this was NOT an initial sync!
                            localEntity.blobName?.let { bName -> blobStore.deleteBlob(bName) }
                            vaultEntryDao.deleteEntry(localEntity)
                        } else if (localEntity.syncStatus == SyncStatus.PENDING.name || isFirstSync) {
                            // New local file or initial upload to Drive
                            localEntity.blobName?.let { bName ->
                                val bFile = blobStore.getBlobFile(bName)
                                if (bFile.exists()) {
                                    driveClient.uploadBlob(bName, bFile)
                                }
                            }
                            vaultEntryDao.updateEntry(localEntity.copy(syncStatus = SyncStatus.SYNCED.name))
                        }
                    } else if (localEntity.syncStatus == SyncStatus.PENDING.name) {
                        // Local modifications pending upload
                        localEntity.blobName?.let { bName ->
                            val bFile = blobStore.getBlobFile(bName)
                            if (bFile.exists()) {
                                driveClient.uploadBlob(bName, bFile)
                            }
                        }
                        vaultEntryDao.updateEntry(localEntity.copy(syncStatus = SyncStatus.SYNCED.name))
                    }
                }

                // Step 3: Export consolidated encrypted index, upload to Google Drive, and persist locally
                val mergedIndexData = encryptedIndex.exportToEncryptedData(masterKey)
                driveClient.uploadIndex(mergedIndexData)
                encryptedIndex.saveIndex(masterKey)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Bi-directional sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getSignInIntent(context: android.content.Context): android.content.Intent? {
        return driveClient.getSignInIntent()
    }

    override fun handleSignInResult(data: android.content.Intent?): Result<String> {
        return driveClient.handleSignInResult(data)
    }

    override fun setManualAccount(email: String) {
        driveClient.setManualEmail(email)
    }

    override fun signOut(): Boolean {
        return driveClient.signOut()
    }

    companion object {
        private const val TAG = "SyncEngine"
    }
}
