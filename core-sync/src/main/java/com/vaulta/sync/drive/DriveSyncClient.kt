package com.vaulta.sync.drive

import android.content.Intent
import com.vaulta.vault.sync.CloudStorageStats
import java.io.File

interface DriveSyncClient {

    suspend fun uploadBlob(blobName: String, file: File): Boolean

    suspend fun downloadBlob(blobName: String, destinationFile: File): Boolean

    suspend fun deleteBlob(blobName: String): Boolean

    suspend fun deleteBlobById(fileId: String): Boolean = false

    suspend fun uploadIndex(encryptedIndexData: ByteArray): Boolean

    suspend fun downloadIndex(allowAutoDiscovery: Boolean = false): ByteArray?

    suspend fun deleteOldStorageSpace(wasStealth: Boolean): Boolean = true

    suspend fun wipeCurrentStorage(): Boolean = false

    suspend fun getCloudStorageStats(): CloudStorageStats? = null

    suspend fun listRemoteBlobs(): List<String>

    fun isSignedIn(): Boolean

    fun getSignInIntent(): Intent

    fun handleSignInResult(data: Intent?): Result<String>

    fun setManualEmail(email: String)

    fun signOut(): Boolean

    fun getSignedInEmail(): String? = null

    fun setStealthMode(enabled: Boolean)

    suspend fun switchStorageSpace(toStealthMode: Boolean): Boolean

    fun getLastError(): String? = null
}
