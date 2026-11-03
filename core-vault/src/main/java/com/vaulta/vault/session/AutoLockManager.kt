package com.vaulta.vault.session

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.vaulta.storage.cleanup.ViewerCachePurge
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.vault.preferences.AppPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic locking of the vault when the app goes to the background.
 *
 * Listens to [ProcessLifecycleOwner] lifecycle events:
 * - On [onStop]: starts a countdown timer. When it expires, calls [VaultSession.lock].
 * - On [onStart]: cancels any pending countdown so the vault stays unlocked.
 */
interface AutoLockManager {

    /**
     * Sets the auto-lock timeout duration in milliseconds.
     *
     * @param timeoutMs Timeout in milliseconds. Must be positive.
     */
    fun setTimeoutMs(timeoutMs: Long)

    /**
     * Starts observing the process lifecycle for auto-lock behavior.
     * Must be called from the main thread (e.g., in Application.onCreate).
     */
    fun start()

    /**
     * Stops observing the process lifecycle and cancels any pending lock timer.
     */
    fun stop()
}

/**
 * Default implementation of [AutoLockManager] using [ProcessLifecycleOwner]
 * and coroutine-based countdown.
 */
@Singleton
class AutoLockManagerImpl @Inject constructor(
    private val vaultSession: VaultSession,
    private val encryptedIndex: EncryptedIndex,
    private val appPreferencesManager: AppPreferencesManager,
    @ApplicationContext private val context: Context,
) : AutoLockManager, DefaultLifecycleObserver {

    private var timeoutMs: Long = DEFAULT_TIMEOUT_MS
    private var countdownJob: Job? = null
    private var collectJob: Job? = null
    private var isStarted = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun setTimeoutMs(timeoutMs: Long) {
        require(timeoutMs > 0) { "Timeout must be positive, was: $timeoutMs" }
        this.timeoutMs = timeoutMs
        val minutes = (timeoutMs / (60L * 1000L)).toInt().coerceAtLeast(1)
        appPreferencesManager.setAutoLockTimeoutMinutes(minutes)
    }

    override fun start() {
        if (isStarted) return
        isStarted = true

        timeoutMs = appPreferencesManager.autoLockTimeoutMinutes.value * 60L * 1000L
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        collectJob?.cancel()
        collectJob = scope.launch(Dispatchers.IO) {
            vaultSession.isUnlocked.collect { unlocked ->
                if (!unlocked) {
                    ViewerCachePurge.purgeViewerTemp(context)
                    try {
                        encryptedIndex.clear()
                    } catch (e: Exception) {
                        Log.e("AutoLockManager", "Failed to clear in-memory index on lock", e)
                    }
                }
            }
        }
    }

    override fun stop() {
        if (!isStarted) return
        isStarted = false
        cancelCountdown()
        collectJob?.cancel()
        collectJob = null
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        ViewerCachePurge.purgeViewerTemp(context)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground — cancel any pending lock countdown
        cancelCountdown()
    }

    override fun onStop(owner: LifecycleOwner) {
        // App went to background — start the auto-lock countdown
        if (vaultSession.isUnlocked.value) {
            startCountdown()
        }
    }

    private fun startCountdown() {
        cancelCountdown()
        countdownJob = scope.launch {
            delay(timeoutMs)
            vaultSession.lock()
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    companion object {
        /** Default auto-lock timeout: 5 minutes. */
        const val DEFAULT_TIMEOUT_MS = 5L * 60L * 1000L
    }
}
