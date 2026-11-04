package com.vaulta.vault.sync

import android.content.Context
import android.content.Intent

interface SyncProvider {
    val isAvailable: Boolean
    val providerName: String
    suspend fun sync(): Result<Unit>
    fun getSignInIntent(context: Context): Intent? = null
    fun handleSignInResult(data: Intent?): Result<String> = Result.failure(UnsupportedOperationException("Sign in not supported"))
    fun setManualAccount(email: String) {}
    fun signOut(): Boolean = false
    fun setStealthMode(enabled: Boolean) {}
    suspend fun switchStorageSpace(toStealthMode: Boolean): Boolean = false
    suspend fun downloadSpecificBlob(id: String): Result<Unit> = Result.failure(UnsupportedOperationException())
    suspend fun removeRemoteBlob(id: String): Result<Unit> = Result.failure(UnsupportedOperationException())
    suspend fun downloadAllRemoteBlobs(): Result<Unit> = Result.failure(UnsupportedOperationException())
}
