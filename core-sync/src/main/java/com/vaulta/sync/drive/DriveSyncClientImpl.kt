package com.vaulta.sync.drive

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveSyncClientImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DriveSyncClient {

    private var manualUserEmail: String? = null

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val email = account?.email ?: manualUserEmail ?: return null

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        if (account?.account != null) {
            credential.selectedAccount = account.account
        } else {
            credential.selectedAccountName = email
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Vaulta").build()
    }

    override suspend fun uploadBlob(blobName: String, file: File): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        val drive = getDriveService() ?: return@withContext false

        try {
            val existing = findFileByName(drive, blobName)
            val mediaContent = FileContent("application/octet-stream", file)
            if (existing != null) {
                drive.files().update(existing.id, null, mediaContent).execute()
            } else {
                val metadata = DriveFile().apply {
                    name = blobName
                    parents = listOf("appDataFolder")
                }
                drive.files().create(metadata, mediaContent).execute()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading blob $blobName to Google Drive", e)
            false
        }
    }

    override suspend fun downloadBlob(blobName: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext false
        try {
            val remoteFile = findFileByName(drive, blobName) ?: return@withContext false
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { os ->
                drive.files().get(remoteFile.id).executeMediaAndDownloadTo(os)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading blob $blobName from Google Drive", e)
            false
        }
    }

    override suspend fun deleteBlob(blobName: String): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext true
        try {
            val remoteFile = findFileByName(drive, blobName) ?: return@withContext true
            drive.files().delete(remoteFile.id).execute()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting blob $blobName from Google Drive", e)
            false
        }
    }

    override suspend fun uploadIndex(encryptedIndexData: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext false
        try {
            val existing = findFileByName(drive, INDEX_FILE_NAME)
            val mediaContent = ByteArrayContent("application/octet-stream", encryptedIndexData)
            if (existing != null) {
                drive.files().update(existing.id, null, mediaContent).execute()
            } else {
                val metadata = DriveFile().apply {
                    name = INDEX_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                drive.files().create(metadata, mediaContent).execute()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading index to Google Drive", e)
            false
        }
    }

    override suspend fun downloadIndex(): ByteArray? = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext null
        try {
            val remoteFile = findFileByName(drive, INDEX_FILE_NAME) ?: return@withContext null
            ByteArrayOutputStream().use { os ->
                drive.files().get(remoteFile.id).executeMediaAndDownloadTo(os)
                os.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading index from Google Drive", e)
            null
        }
    }

    override suspend fun listRemoteBlobs(): List<String> = withContext(Dispatchers.IO) {
        val drive = getDriveService() ?: return@withContext emptyList()
        try {
            val list = drive.files().list()
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute()
            list.files?.mapNotNull { it.name } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed listing remote files from Google Drive", e)
            emptyList()
        }
    }

    override fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null || manualUserEmail != null
    }

    override fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    override fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: "Google Drive User"
            Result.success(email)
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed with status code ${e.statusCode}", e)
            val msg = if (e.statusCode == 10) {
                "Developer Error 10: App SHA-1 fingerprint needs to be added to Google Cloud Console for 'com.vaulta.shrs'."
            } else {
                "Google Sign-In failed (Status Code: ${e.statusCode})"
            }
            Result.failure(IllegalStateException(msg))
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    override fun setManualEmail(email: String) {}

    override fun signOut(): Boolean {
        return try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(context, gso).signOut()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun findFileByName(drive: Drive, name: String): DriveFile? {
        val query = "name = '$name' and 'appDataFolder' in parents and trashed = false"
        val result = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ(query)
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()
    }

    companion object {
        private const val TAG = "DriveSyncClient"
        private const val INDEX_FILE_NAME = "vault_index.enc"
        private const val WEB_CLIENT_ID = "305609659614-8fa695al6po3gbllhe97247nk9pie6se.apps.googleusercontent.com"
    }
}
