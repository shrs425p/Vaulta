package com.vaulta.sync.drive

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import com.vaulta.vault.preferences.AppPreferencesManager
import com.vaulta.vault.sync.CloudBlobInfo
import com.vaulta.vault.sync.CloudStorageStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveSyncClientImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferencesManager: AppPreferencesManager
) : DriveSyncClient {

    private var cachedFolderId: String? = null
    @Volatile
    private var isSessionInvalidated = false
    @Volatile
    private var lastError: String? = null

    private val isStealthMode: Boolean
        get() = appPreferencesManager.isStealthSyncEnabled.value

    private fun getDriveService(): Drive? {
        if (isSessionInvalidated) return null
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA)
        )
        if (account.account != null) {
            credential.selectedAccount = account.account
        } else if (!account.email.isNullOrBlank()) {
            credential.selectedAccountName = account.email
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Vaulta").build()
    }

    private fun getTargetParentFolderId(drive: Drive): String {
        return if (isStealthMode) {
            "appDataFolder"
        } else {
            getOrCreateVaultFolder(drive)
        }
    }

    private fun getOrCreateVaultFolder(drive: Drive): String {
        cachedFolderId?.let { return it }

        val query = "name = '$VAULT_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result = drive.files().list()
            .setSpaces("drive")
            .setQ(query)
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()

        val existingFolder = result.files?.firstOrNull()
        if (existingFolder != null) {
            cachedFolderId = existingFolder.id
            return existingFolder.id
        }

        val folderMetadata = DriveFile().apply {
            name = VAULT_FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }
        val newFolder = drive.files().create(folderMetadata).setFields("id").execute()
        cachedFolderId = newFolder.id
        return newFolder.id
    }

    private fun handleApiError(e: Exception) {
        when (e) {
            is GoogleJsonResponseException -> {
                val statusCode = e.statusCode
                val errorDetails = e.details?.message ?: e.message
                val reason = when (statusCode) {
                    401 -> "Authentication error (HTTP 401): Stale or revoked Google credentials. Please sign in again."
                    403 -> "Google Drive access forbidden (HTTP 403): $errorDetails. Make sure the 'Google Drive API' is enabled in your Google Cloud Console."
                    404 -> "Google Drive file not found (HTTP 404): $errorDetails"
                    else -> "Google Drive API error (HTTP $statusCode): $errorDetails"
                }
                lastError = reason
                Log.e(TAG, "GoogleJsonResponseException: $reason", e)
                if (statusCode == 401) {
                    isSessionInvalidated = true
                    signOut()
                }
            }
            else -> {
                lastError = "Network error: ${e.message ?: "Unknown error"}"
                Log.e(TAG, "Api error: ${e.message}", e)
            }
        }
    }

    override suspend fun uploadBlob(blobName: String, file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            lastError = "Local file does not exist for blob $blobName"
            return@withContext false
        }
        val drive = getDriveService()
        if (drive == null) {
            lastError = "Google Drive account is not signed in."
            return@withContext false
        }

        try {
            val folderId = getTargetParentFolderId(drive)
            val existing = findFileByName(drive, folderId, blobName)
            val mediaContent = FileContent("application/octet-stream", file)
            if (existing != null) {
                val req = drive.files().update(existing.id, null, mediaContent)
                req.mediaHttpUploader.isDirectUploadEnabled = false
                req.mediaHttpUploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE * 4) // 1MB chunks
                req.execute()
            } else {
                val metadata = DriveFile().apply {
                    name = blobName
                    parents = listOf(folderId)
                }
                val req = drive.files().create(metadata, mediaContent)
                req.mediaHttpUploader.isDirectUploadEnabled = false
                req.mediaHttpUploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE * 4) // 1MB chunks
                req.execute()
            }
            lastError = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading blob $blobName (${file.length()} bytes) to Google Drive", e)
            handleApiError(e)
            false
        }
    }

    override suspend fun downloadBlob(blobName: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService()
        if (drive == null) {
            lastError = "Google Drive account is not signed in."
            return@withContext false
        }
        try {
            val folderId = getTargetParentFolderId(drive)
            var remoteFile = queryFileInSpace(drive, isStealthMode, folderId, blobName)
            // Cross-space fallback: check alternate space in case blob was uploaded under the other storage mode
            if (remoteFile == null) {
                val altStealth = !isStealthMode
                val altFolderId = if (altStealth) "appDataFolder" else getOrCreateVaultFolder(drive)
                remoteFile = queryFileInSpace(drive, altStealth, altFolderId, blobName)
            }
            if (remoteFile == null) {
                lastError = "Remote blob $blobName not found on Google Drive"
                return@withContext false
            }
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { os ->
                val req = drive.files().get(remoteFile.id)
                req.mediaHttpDownloader.isDirectDownloadEnabled = false
                req.executeMediaAndDownloadTo(os)
            }
            lastError = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading blob $blobName from Google Drive", e)
            handleApiError(e)
            false
        }
    }

    override suspend fun deleteBlob(blobName: String): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext true
        try {
            val folderId = getTargetParentFolderId(drive)
            val primaryFile = queryFileInSpace(drive, isStealthMode, folderId, blobName)
            if (primaryFile != null) {
                drive.files().delete(primaryFile.id).execute()
            }
            // Clean up from alternate space if present
            val altStealth = !isStealthMode
            val altFolderId = if (altStealth) "appDataFolder" else getOrCreateVaultFolder(drive)
            val altFile = queryFileInSpace(drive, altStealth, altFolderId, blobName)
            if (altFile != null) {
                drive.files().delete(altFile.id).execute()
            }
            lastError = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting blob $blobName from Google Drive", e)
            handleApiError(e)
            false
        }
    }

    override suspend fun uploadIndex(encryptedIndexData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService()
        if (drive == null) {
            lastError = "Google Drive account is not signed in."
            return@withContext false
        }
        try {
            val folderId = getTargetParentFolderId(drive)
            val existing = findFileByName(drive, folderId, INDEX_FILE_NAME)
            val mediaContent = ByteArrayContent("application/octet-stream", encryptedIndexData)
            if (existing != null) {
                drive.files().update(existing.id, null, mediaContent).execute()
            } else {
                val metadata = DriveFile().apply {
                    name = INDEX_FILE_NAME
                    parents = listOf(folderId)
                }
                drive.files().create(metadata, mediaContent).execute()
            }
            lastError = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading index to Google Drive", e)
            handleApiError(e)
            false
        }
    }

    override suspend fun downloadIndex(allowAutoDiscovery: Boolean): ByteArray? = withContext(Dispatchers.IO) {
        val drive = getDriveService()
        if (drive == null) {
            lastError = "Google Drive account is not signed in."
            return@withContext null
        }
        try {
            val folderId = getTargetParentFolderId(drive)
            var remoteFile = queryFileInSpace(drive, isStealthMode, folderId, INDEX_FILE_NAME)

            // DUAL-SPACE AUTO-DISCOVERY:
            // Only search alternate space if explicitly permitted (e.g. restoring an empty vault on a fresh install)
            // Never override user's active choice during normal sync or manual space migration!
            if (remoteFile == null && allowAutoDiscovery) {
                val alternateStealth = !isStealthMode
                val altFolderId = if (alternateStealth) "appDataFolder" else getOrCreateVaultFolder(drive)
                val altFile = queryFileInSpace(drive, alternateStealth, altFolderId, INDEX_FILE_NAME)
                if (altFile != null) {
                    Log.i(TAG, "Vault index discovered in alternate space (Stealth=$alternateStealth). Auto-aligning mode.")
                    appPreferencesManager.setIsStealthSyncEnabled(alternateStealth)
                    remoteFile = altFile
                }
            }

            if (remoteFile == null) {
                lastError = null
                return@withContext null
            }
            val bytes = ByteArrayOutputStream().use { os ->
                drive.files().get(remoteFile.id).executeMediaAndDownloadTo(os)
                os.toByteArray()
            }
            lastError = null
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading index from Google Drive", e)
            handleApiError(e)
            null
        }
    }

    override suspend fun deleteOldStorageSpace(wasStealth: Boolean): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext false
        try {
            if (wasStealth) {
                // Old space was Stealth: remove index from appDataFolder
                val list = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name = '$INDEX_FILE_NAME' and 'appDataFolder' in parents and trashed = false")
                    .setFields("files(id)")
                    .execute()
                list.files?.forEach { file ->
                    try { drive.files().delete(file.id).execute() } catch (_: Exception) {}
                }
            } else {
                // Old space was Visible: remove old Vaulta Backups folder
                val query = "name = '$VAULT_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
                val result = drive.files().list()
                    .setSpaces("drive")
                    .setQ(query)
                    .setFields("files(id)")
                    .execute()
                result.files?.forEach { folder ->
                    try { drive.files().delete(folder.id).execute() } catch (_: Exception) {}
                }
                cachedFolderId = null
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed cleaning up old storage space (wasStealth=$wasStealth): ${e.message}")
            false
        }
    }

    override suspend fun listRemoteBlobs(): List<String> = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext emptyList()
        try {
            val space = if (isStealthMode) "appDataFolder" else "drive"
            val folderId = if (isStealthMode) "appDataFolder" else getOrCreateVaultFolder(drive)
            val query = if (isStealthMode) {
                "'appDataFolder' in parents and trashed = false"
            } else {
                "'$folderId' in parents and trashed = false"
            }
            var list = drive.files().list()
                .setSpaces(space)
                .setQ(query)
                .setFields("files(id, name)")
                .execute()
                .files ?: emptyList()

            // If active space has no files, check alternate space
            if (list.isEmpty()) {
                val altStealth = !isStealthMode
                val altSpace = if (altStealth) "appDataFolder" else "drive"
                val altFolderId = if (altStealth) "appDataFolder" else getOrCreateVaultFolder(drive)
                val altQuery = if (altStealth) {
                    "'appDataFolder' in parents and trashed = false"
                } else {
                    "'$altFolderId' in parents and trashed = false"
                }
                val altList = drive.files().list()
                    .setSpaces(altSpace)
                    .setQ(altQuery)
                    .setFields("files(id, name)")
                    .execute()
                    .files ?: emptyList()
                if (altList.isNotEmpty()) {
                    list = altList
                }
            }

            lastError = null
            list.mapNotNull { it.name }
        } catch (e: Exception) {
            Log.e(TAG, "Failed listing remote files from Google Drive", e)
            handleApiError(e)
            emptyList()
        }
    }

    override suspend fun deleteBlobById(fileId: String): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext false
        try {
            drive.files().delete(fileId).execute()
            lastError = null
            true
        } catch (e: Exception) {
            handleApiError(e)
            false
        }
    }

    override suspend fun wipeCurrentStorage(): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext false
        try {
            if (isStealthMode) {
                val list = drive.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("'appDataFolder' in parents and trashed = false")
                    .setFields("files(id)")
                    .execute()
                list.files?.forEach { file ->
                    try { drive.files().delete(file.id).execute() } catch (_: Exception) {}
                }
            } else {
                val query = "name = '$VAULT_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
                val result = drive.files().list()
                    .setSpaces("drive")
                    .setQ(query)
                    .setFields("files(id)")
                    .execute()
                result.files?.forEach { folder ->
                    try { drive.files().delete(folder.id).execute() } catch (_: Exception) {}
                }
                cachedFolderId = null
            }
            lastError = null
            true
        } catch (e: Exception) {
            handleApiError(e)
            false
        }
    }

    override suspend fun getCloudStorageStats(): CloudStorageStats? = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext null
        try {
            val space = if (isStealthMode) "appDataFolder" else "drive"
            val folderId = if (isStealthMode) "appDataFolder" else getOrCreateVaultFolder(drive)
            val query = if (isStealthMode) {
                "'appDataFolder' in parents and trashed = false"
            } else {
                "'$folderId' in parents and trashed = false"
            }
            val result = drive.files().list()
                .setSpaces(space)
                .setQ(query)
                .setFields("files(id, name, size, modifiedTime)")
                .execute()

            val files = result.files ?: emptyList()
            var totalSize = 0L
            val blobList = files.map { f ->
                val size = f.getSize() ?: 0L
                totalSize += size
                val modTime = f.modifiedTime?.value ?: 0L
                CloudBlobInfo(
                    id = f.id,
                    name = f.name ?: "Unknown",
                    sizeBytes = size,
                    modifiedTimeMs = modTime
                )
            }
            lastError = null
            CloudStorageStats(
                totalFiles = blobList.size,
                totalSizeBytes = totalSize,
                isStealthSpace = isStealthMode,
                blobs = blobList.sortedByDescending { it.sizeBytes }
            )
        } catch (e: Exception) {
            handleApiError(e)
            null
        }
    }

    override fun setStealthMode(enabled: Boolean) {
        appPreferencesManager.setIsStealthSyncEnabled(enabled)
    }

    override suspend fun switchStorageSpace(toStealthMode: Boolean): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext false
        try {
            cachedFolderId = null
            appPreferencesManager.setIsStealthSyncEnabled(toStealthMode)
            lastError = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed switching storage space", e)
            handleApiError(e)
            false
        }
    }

    override fun isSignedIn(): Boolean {
        if (isSessionInvalidated) return false
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }

    override fun getSignedInEmail(): String? {
        if (isSessionInvalidated) return null
        return GoogleSignIn.getLastSignedInAccount(context)?.email
    }

    override fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    override fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: "Google Drive User"
            isSessionInvalidated = false
            lastError = null
            Result.success(email)
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed with status code ${e.statusCode}", e)
            val msg = if (e.statusCode == 10) {
                "Developer Error 10: App SHA-1 fingerprint needs to be added to Google Cloud Console for 'com.vaulta.shrs'."
            } else {
                "Google Sign-In failed (Status Code: ${e.statusCode})"
            }
            lastError = msg
            Result.failure(IllegalStateException(msg))
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            lastError = e.message
            Result.failure(e)
        }
    }

    override fun setManualEmail(email: String) {}

    override fun signOut(): Boolean {
        cachedFolderId = null
        isSessionInvalidated = true
        lastError = null
        return try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val client = GoogleSignIn.getClient(context, gso)
            client.signOut()
            client.revokeAccess()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getLastError(): String? = lastError

    private fun queryFileInSpace(drive: Drive, inStealth: Boolean, folderId: String, name: String): DriveFile? {
        val space = if (inStealth) "appDataFolder" else "drive"
        val query = if (inStealth) {
            "name = '$name' and 'appDataFolder' in parents and trashed = false"
        } else {
            "name = '$name' and '$folderId' in parents and trashed = false"
        }
        val result = drive.files().list()
            .setSpaces(space)
            .setQ(query)
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()
    }

    private fun findFileByName(drive: Drive, folderId: String, name: String): DriveFile? {
        return queryFileInSpace(drive, isStealthMode, folderId, name)
    }

    companion object {
        private const val TAG = "DriveSyncClient"
        private const val VAULT_FOLDER_NAME = "Vaulta Backups"
        private const val INDEX_FILE_NAME = "vault_index.enc"
    }
}
