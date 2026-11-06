package com.vaulta.vault.preferences

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyFilePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences by lazy {
        val encryptedPrefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "vaulta_secure_key_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            try { Log.w(TAG, "EncryptedSharedPreferences unavailable") } catch (_: Throwable) {}
            null
        }

        if (encryptedPrefs != null) {
            // Migrate unencrypted data if present
            try {
                val legacyPrefs = context.getSharedPreferences("vaulta_key_prefs", Context.MODE_PRIVATE)
                val legacyUri = legacyPrefs.getString(KEY_FILE_URI, null)
                if (legacyUri != null && !encryptedPrefs.contains(KEY_FILE_URI)) {
                    encryptedPrefs.edit().putString(KEY_FILE_URI, legacyUri).apply()
                    legacyPrefs.edit().clear().apply()
                    try { Log.i(TAG, "Migrated key file URI to EncryptedSharedPreferences") } catch (_: Throwable) {}
                }
            } catch (e: Exception) {
                try { Log.w(TAG, "Failed migration of legacy key prefs") } catch (_: Throwable) {}
            }
            encryptedPrefs
        } else {
            context.getSharedPreferences("vaulta_key_prefs", Context.MODE_PRIVATE)
        }
    }

    /**
     * Takes persistable URI read/write permissions from the content resolver
     * and saves the URI for seamless auto-detection on future unlocks.
     */
    fun saveKeyFileUri(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                // If write permission cannot be persisted, fall back to read permission
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed taking persistable key-file URI permission")
        }

        prefs.edit().putString(KEY_FILE_URI, uri.toString()).apply()
        Log.d(TAG, "Saved persistable key-file URI")
    }

    /**
     * Returns the remembered vault.key URI if still accessible, or null if revoked/missing.
     */
    fun getSavedKeyFileUri(): Uri? {
        val uriStr = prefs.getString(KEY_FILE_URI, null) ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            // Verify permission and accessibility
            context.contentResolver.openInputStream(uri)?.close()
            uri
        } catch (e: Exception) {
            Log.w(TAG, "Saved key-file URI is no longer accessible")
            null
        }
    }

    /**
     * Resolves the display file name of the key file (e.g., 'vault.key').
     */
    fun getFileName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "vault.key"
        } catch (e: Exception) {
            "vault.key"
        }
    }

    /**
     * Clears the remembered key file location.
     */
    fun clearKeyFileUri() {
        prefs.edit().remove(KEY_FILE_URI).apply()
    }

    fun getFailedUnlockAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun getLockoutRemainingSeconds(): Long {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val remaining = until - System.currentTimeMillis()
        return if (remaining > 0L) (remaining + 999) / 1000 else 0L
    }

    fun recordFailedUnlockAttempt(): Long {
        val attempts = getFailedUnlockAttempts() + 1
        val lockoutDurationMs = when {
            attempts >= 10 -> 300_000L // 5 minutes
            attempts >= 5 -> 30_000L   // 30 seconds
            attempts >= 3 -> 5_000L    // 5 seconds
            else -> 0L
        }

        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
        if (lockoutDurationMs > 0L) {
            editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + lockoutDurationMs)
        }
        editor.apply()
        return lockoutDurationMs / 1000
    }

    fun resetFailedUnlockAttempts() {
        prefs.edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    companion object {
        private const val TAG = "KeyFilePrefManager"
        private const val KEY_FILE_URI = "saved_vault_key_uri"
        private const val KEY_FAILED_ATTEMPTS = "unlock_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "unlock_lockout_until_timestamp"
    }
}
