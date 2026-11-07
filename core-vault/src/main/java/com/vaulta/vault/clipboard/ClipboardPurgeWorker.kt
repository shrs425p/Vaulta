package com.vaulta.vault.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ClipboardPurgeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val ownershipLabel = inputData.getString(KEY_OWNERSHIP_LABEL) ?: return Result.success()

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return Result.success()

        try {
            val primaryClip = clipboardManager.primaryClip
            if (primaryClip != null && primaryClip.itemCount > 0) {
                val label = primaryClip.description.label?.toString()
                if (label == ownershipLabel) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboardManager.clearPrimaryClip()
                    } else {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            }
        } catch (_: SecurityException) {
        }

        return Result.success()
    }

    companion object {
        const val KEY_OWNERSHIP_LABEL = "ownership_label"
    }
}
