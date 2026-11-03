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
}
