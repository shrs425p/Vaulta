package com.vaulta.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.vaulta.sync.SyncEngine
import com.vaulta.vault.session.VaultSession
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class VaultSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEngine: SyncEngine,
    private val vaultSession: VaultSession
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!vaultSession.isUnlocked.value) {
            // Cannot sync when the vault is locked. Return failure immediately
            // to stop WorkManager from retrying endlessly in the background and wasting battery.
            return Result.failure()
        }

        val result = syncEngine.performSync()
        return if (result.isSuccess) {
            Result.success()
        } else {
            val ex = result.exceptionOrNull()
            if (ex is IllegalStateException && ex.message == "Vault is locked") {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        const val WORK_NAME = "VaultSyncWorker"

        fun createOneTimeWorkRequest(): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            return OneTimeWorkRequestBuilder<VaultSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        }

        fun createPeriodicWorkRequest(repeatIntervalHours: Long = 1): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            return PeriodicWorkRequestBuilder<VaultSyncWorker>(repeatIntervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        }
    }
}
