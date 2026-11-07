package com.vaulta.vault.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardAutoPurgeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var currentOwnershipLabel: String? = null

    /**
     * Copies sensitive [text] to the system clipboard and schedules an automatic wipe after [purgeDelayMs].
     *
     * @param label Descriptive label for the clip (e.g. "Password", "TOTP Code").
     * @param text The secret payload to copy.
     * @param purgeDelayMs Auto-clear timeout in milliseconds (defaults to 30,000ms / 30 seconds).
     */
    fun copySensitiveText(
        label: String,
        text: String,
        purgeDelayMs: Long = 30_000L
    ) {
        val ownershipLabel = "$OWNERSHIP_PREFIX$label:${java.util.UUID.randomUUID()}"
        currentOwnershipLabel = ownershipLabel
        val clipData = ClipData.newPlainText(ownershipLabel, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clipData.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboardManager.setPrimaryClip(clipData)

        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(PURGE_WORK_TAG)

        val purgeRequest = OneTimeWorkRequestBuilder<ClipboardPurgeWorker>()
            .setInitialDelay(purgeDelayMs, TimeUnit.MILLISECONDS)
            .addTag(PURGE_WORK_TAG)
            .setInputData(
                Data.Builder()
                    .putString(ClipboardPurgeWorker.KEY_OWNERSHIP_LABEL, ownershipLabel)
                    .build()
            )
            .build()

        workManager.enqueue(purgeRequest)
    }

    fun clearOwnedClip() {
        try {
            val label = clipboardManager.primaryClip?.description?.label?.toString()
            if (label?.startsWith(OWNERSHIP_PREFIX) == true) {
                clearPrimaryClip()
                currentOwnershipLabel = null
            }
        } catch (_: SecurityException) {
        }
    }

    private fun clearIfOwned(ownershipLabel: String) {
        try {
            val primaryClip = clipboardManager.primaryClip
            if (primaryClip != null && primaryClip.itemCount > 0) {
                val label = primaryClip.description.label?.toString()
                if (label == ownershipLabel) {
                    clearPrimaryClip()
                }
            }
        } catch (_: SecurityException) {
        }
    }

    private fun clearPrimaryClip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboardManager.clearPrimaryClip()
        } else {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    companion object {
        private const val OWNERSHIP_PREFIX = "VaultaSensitive:"
        private const val PURGE_WORK_TAG = "ClipboardPurgeWork"
    }
}
