package com.vaulta.sync

import android.util.Log
import com.vaulta.storage.blob.BlobStore
import com.vaulta.storage.index.VaultEntryDao
import com.vaulta.storage.index.toEntity
import com.vaulta.storage.index.toDomain
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.storage.index.SyncStatus
import com.vaulta.storage.index.SyncPolicy
import com.vaulta.sync.drive.DriveSyncClient
import com.vaulta.vault.session.VaultSession
import com.vaulta.vault.sync.CloudStorageStats
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

    private var pendingCleanupOldSpaceWasStealth: Boolean? = null

    suspend fun performSync(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!driveClient.isSignedIn()) {
            return@withContext Result.failure(IllegalStateException("Google Drive not signed in"))
        }

        if (!vaultSession.isUnlocked.value) {
            return@withContext Result.failure(IllegalStateException("Vault is locked"))
        }

        try {
            vaultSession.withMasterKey { masterKey ->
                val localEntitiesBefore = vaultEntryDao.getAllEntries()
                val isLocalEmpty = localEntitiesBefore.none { it.syncStatus != SyncStatus.DELETED.name }

                // Step 0: Download remote index from Google Drive and merge remote changes
                Log.d(TAG, "Step 0: Checking and downloading remote index from Google Drive (allowAutoDiscovery=$isLocalEmpty)...")
                val remoteIndexBytes = driveClient.downloadIndex(allowAutoDiscovery = isLocalEmpty)
                val clientError = driveClient.getLastError()
                if (clientError != null) {
                    throw IllegalStateException(clientError)
                }

                val isFirstSync = (remoteIndexBytes == null || remoteIndexBytes.isEmpty())
                val remoteEntries = if (!isFirstSync) {
                    try {
                        encryptedIndex.parseFromEncryptedData(remoteIndexBytes!!, masterKey)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed decrypting remote index; sync aborted")
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

                // Step 1: Process Remote Delta -> Merge into local Room DB (On-demand blobs)
                for (remoteEntry in remoteEntries) {
                    val localEntity = localMap[remoteEntry.id]

                    if (localEntity == null) {
                        // Entry exists remotely but not in local database
                        if (locallyDeletedIds.contains(remoteEntry.id)) {
                            // User on this device explicitly deleted it; sync deletion to remote
                            remoteEntry.blobName?.let { driveClient.deleteBlob(it) }
                        } else {
                            // New file added from cloud -> Register entry with on-demand availability
                            val isBlobPresent = if (!remoteEntry.blobName.isNullOrBlank() && !remoteEntry.isDirectory) {
                                blobStore.getBlobFile(remoteEntry.blobName!!).exists()
                            } else {
                                true
                            }
                            vaultEntryDao.insertEntry(
                                remoteEntry.copy(
                                    syncStatus = SyncStatus.SYNCED,
                                    isAvailableLocally = isBlobPresent
                                ).toEntity()
                            )
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
                                    if (bFile.exists() && localEntity.syncPolicy != SyncPolicy.LOCAL_ONLY.name) {
                                        val uploaded = driveClient.uploadBlob(bName, bFile)
                                        if (!uploaded) {
                                            val err = driveClient.getLastError() ?: "Failed uploading $bName"
                                            throw IllegalStateException(err)
                                        }
                                    }
                                }

                                val isBlobPresent = remoteEntry.blobName?.let { blobStore.getBlobFile(it).exists() } ?: true
                                vaultEntryDao.insertEntry(
                                    remoteEntry.copy(
                                        syncStatus = SyncStatus.SYNCED,
                                        isAvailableLocally = isBlobPresent
                                    ).toEntity()
                                )
                            }

                            localEntity.syncStatus == SyncStatus.SYNCED.name &&
                                    remoteEntry.modifiedAt > localEntity.modifiedAt -> {
                                // Remote metadata is newer; update local entry
                                val isBlobPresent = remoteEntry.blobName?.let { blobStore.getBlobFile(it).exists() } ?: true
                                vaultEntryDao.updateEntry(
                                    remoteEntry.copy(
                                        syncStatus = SyncStatus.SYNCED,
                                        isAvailableLocally = isBlobPresent
                                    ).toEntity()
                                )
                            }
                        }
                    }
                }

                // Step 2: Process Local Delta -> Remote (Respect LOCAL_ONLY policy)
                val refreshedLocalEntities = vaultEntryDao.getAllEntries()
                for (localEntity in refreshedLocalEntities) {
                    if (localEntity.syncPolicy == SyncPolicy.LOCAL_ONLY.name) {
                        // Skip uploading files marked as LOCAL_ONLY
                        continue
                    }

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
                                    val uploaded = driveClient.uploadBlob(bName, bFile)
                                    if (!uploaded) {
                                        val err = driveClient.getLastError() ?: "Failed uploading $bName"
                                        throw IllegalStateException(err)
                                    }
                                }
                            }
                            vaultEntryDao.updateEntry(localEntity.copy(syncStatus = SyncStatus.SYNCED.name))
                        }
                    } else if (localEntity.syncStatus == SyncStatus.PENDING.name) {
                        // Local modifications pending upload
                        localEntity.blobName?.let { bName ->
                            val bFile = blobStore.getBlobFile(bName)
                            if (bFile.exists()) {
                                val uploaded = driveClient.uploadBlob(bName, bFile)
                                if (!uploaded) {
                                    val err = driveClient.getLastError() ?: "Failed uploading $bName"
                                    throw IllegalStateException(err)
                                }
                            }
                        }
                        vaultEntryDao.updateEntry(localEntity.copy(syncStatus = SyncStatus.SYNCED.name))
                    }
                }

                // Step 3: Export consolidated encrypted index, upload to Google Drive, and persist locally
                val mergedIndexData = encryptedIndex.exportToEncryptedData(masterKey)
                val indexUploaded = driveClient.uploadIndex(mergedIndexData)
                if (!indexUploaded) {
                    val err = driveClient.getLastError() ?: "Failed uploading encrypted index to Google Drive"
                    throw IllegalStateException(err)
                }
                encryptedIndex.saveIndex(masterKey)

                // If this sync was migrating from one storage space to another, clean up old space!
                pendingCleanupOldSpaceWasStealth?.let { oldWasStealth ->
                    try { driveClient.deleteOldStorageSpace(oldWasStealth) } catch (_: Throwable) {}
                    pendingCleanupOldSpaceWasStealth = null
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Bi-directional sync failed")
            Result.failure(e)
        }
    }

    override suspend fun downloadSpecificBlob(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!driveClient.isSignedIn()) {
            return@withContext Result.failure(IllegalStateException("Google Drive account is not signed in."))
        }
        val entity = vaultEntryDao.getEntryById(id) ?: return@withContext Result.failure(IllegalArgumentException("Entry not found"))
        val domain = entity.toDomain()
        val bName = domain.blobName ?: return@withContext Result.failure(IllegalStateException("File has no associated blob"))

        val destBlob = blobStore.getBlobFile(bName)
        if (!destBlob.exists()) {
            val downloaded = driveClient.downloadBlob(bName, destBlob)
            if (!downloaded) {
                val err = driveClient.getLastError() ?: "Failed downloading blob $bName from Google Drive"
                return@withContext Result.failure(IllegalStateException(err))
            }
        }
        vaultEntryDao.updateEntry(entity.copy(isAvailableLocally = true))
        Result.success(Unit)
    }

    override suspend fun removeRemoteBlob(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val entity = vaultEntryDao.getEntryById(id) ?: return@withContext Result.failure(IllegalArgumentException("Entry not found"))
        val domain = entity.toDomain()
        val bName = domain.blobName

        if (bName.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Entry has no cloud blob to remove."))
        }
        if (!driveClient.isSignedIn()) {
            return@withContext Result.failure(IllegalStateException("Google Drive account is not signed in."))
        }
        if (!driveClient.deleteBlob(bName)) {
            val error = driveClient.getLastError() ?: "Failed removing encrypted blob from Google Drive."
            return@withContext Result.failure(IllegalStateException(error))
        }
        vaultEntryDao.updateEntry(entity.copy(syncPolicy = SyncPolicy.LOCAL_ONLY.name))
        vaultSession.withMasterKey { masterKey ->
            encryptedIndex.saveIndex(masterKey)
        }
        Result.success(Unit)
    }

    override suspend fun downloadAllRemoteBlobs(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!driveClient.isSignedIn()) {
            return@withContext Result.failure(IllegalStateException("Google Drive account is not signed in."))
        }
        val cloudOnlyEntities = vaultEntryDao.getAllEntries().filter { !it.isDirectory && (!it.isAvailableLocally || !blobStore.getBlobFile(it.blobName ?: "").exists()) }
        for (entity in cloudOnlyEntities) {
            val bName = entity.blobName ?: continue
            val destBlob = blobStore.getBlobFile(bName)
            if (!destBlob.exists()) {
                val downloaded = driveClient.downloadBlob(bName, destBlob)
                if (downloaded) {
                    vaultEntryDao.updateEntry(entity.copy(isAvailableLocally = true))
                }
            } else {
                vaultEntryDao.updateEntry(entity.copy(isAvailableLocally = true))
            }
        }
        Result.success(Unit)
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

    override fun getSignedInEmail(): String? {
        return driveClient.getSignedInEmail()
    }

    override fun signOut(): Boolean {
        return driveClient.signOut()
    }

    override fun setStealthMode(enabled: Boolean) {
        driveClient.setStealthMode(enabled)
    }

    override suspend fun switchStorageSpace(toStealthMode: Boolean): Boolean {
        val switched = driveClient.switchStorageSpace(toStealthMode)
        if (switched) {
            // Record old space to clean up after successful upload into the new space
            pendingCleanupOldSpaceWasStealth = !toStealthMode

            // Mark all local active files as PENDING so that the subsequent sync
            // uploads all blobs and index into the newly selected storage space!
            val localEntities = vaultEntryDao.getAllEntries()
            for (entity in localEntities) {
                if (entity.syncStatus != SyncStatus.DELETED.name && entity.syncPolicy != SyncPolicy.LOCAL_ONLY.name) {
                    vaultEntryDao.updateEntry(entity.copy(syncStatus = SyncStatus.PENDING.name))
                }
            }
        }
        return switched
    }

    override suspend fun getCloudStorageStats(): Result<CloudStorageStats> = withContext(Dispatchers.IO) {
        if (!driveClient.isSignedIn()) {
            return@withContext Result.failure(IllegalStateException("Google Drive is not signed in"))
        }
        val stats = driveClient.getCloudStorageStats()
        if (stats != null) {
            Result.success(stats)
        } else {
            Result.failure(IllegalStateException(driveClient.getLastError() ?: "Failed fetching cloud stats"))
        }
    }

    override suspend fun wipeCloudStorage(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!driveClient.isSignedIn()) {
            return@withContext Result.failure(IllegalStateException("Google Drive is not signed in"))
        }
        val wiped = driveClient.wipeCurrentStorage()
        if (wiped) {
            val localEntities = vaultEntryDao.getAllEntries()
            for (entity in localEntities) {
                if (entity.syncStatus != SyncStatus.DELETED.name) {
                    vaultEntryDao.updateEntry(entity.copy(syncStatus = SyncStatus.PENDING.name))
                }
            }
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException(driveClient.getLastError() ?: "Failed wiping cloud storage"))
        }
    }

    override suspend fun deleteRemoteBlobById(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!driveClient.isSignedIn()) {
            return@withContext Result.failure(IllegalStateException("Google Drive is not signed in"))
        }
        val deleted = driveClient.deleteBlobById(fileId)
        if (deleted) Result.success(Unit) else Result.failure(IllegalStateException("Failed deleting file"))
    }

    companion object {
        private const val TAG = "SyncEngine"
    }
}
