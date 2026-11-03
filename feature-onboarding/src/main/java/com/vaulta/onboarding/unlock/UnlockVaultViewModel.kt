package com.vaulta.onboarding.unlock

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulta.crypto.SecureMemory
import com.vaulta.vault.VaultOperations
import com.vaulta.vault.biometric.BiometricUnlockManager
import com.vaulta.vault.preferences.KeyFilePreferenceManager
import com.vaulta.vault.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UnlockUiState {
    data object Idle : UnlockUiState
    data object Unlocking : UnlockUiState
    data object Success : UnlockUiState
    data class Error(val message: String) : UnlockUiState
}

@HiltViewModel
class UnlockVaultViewModel @Inject constructor(
    private val vaultOperations: VaultOperations,
    private val keyFilePreferenceManager: KeyFilePreferenceManager,
    val biometricUnlockManager: BiometricUnlockManager,
    private val vaultSession: VaultSession
) : ViewModel() {

    private val _uiState = MutableStateFlow<UnlockUiState>(UnlockUiState.Idle)
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    fun getSavedKeyFileUri(): Uri? = keyFilePreferenceManager.getSavedKeyFileUri()

    fun getKeyFileName(uri: Uri): String = keyFilePreferenceManager.getFileName(uri)

    fun getLockoutRemainingSeconds(): Long = keyFilePreferenceManager.getLockoutRemainingSeconds()

    fun isBiometricEnabled(): Boolean = biometricUnlockManager.isBiometricEnabled()

    fun canAuthenticateBiometrics(): Boolean = biometricUnlockManager.canAuthenticate()

    fun unlockVault(passwordChars: CharArray, keyFileUri: Uri) {
        val remainingLockout = keyFilePreferenceManager.getLockoutRemainingSeconds()
        if (remainingLockout > 0) {
            SecureMemory.wipe(passwordChars)
            _uiState.value = UnlockUiState.Error(
                "Too many failed attempts. Please wait $remainingLockout seconds before retrying."
            )
            return
        }

        _uiState.value = UnlockUiState.Unlocking
        viewModelScope.launch {
            try {
                keyFilePreferenceManager.saveKeyFileUri(keyFileUri)
                vaultOperations.unlockWithPassword(passwordChars, keyFileUri)
                keyFilePreferenceManager.resetFailedUnlockAttempts()
                _uiState.value = UnlockUiState.Success
            } catch (e: Exception) {
                val lockoutSec = keyFilePreferenceManager.recordFailedUnlockAttempt()
                val attempts = keyFilePreferenceManager.getFailedUnlockAttempts()
                val msg = if (lockoutSec > 0) {
                    "Incorrect credentials ($attempts failed attempts). Locked out for $lockoutSec seconds."
                } else {
                    val remainingBeforeLock = (3 - attempts).coerceAtLeast(0)
                    if (remainingBeforeLock > 0) {
                        "Invalid password or corrupted vault.key ($remainingBeforeLock attempt(s) remaining before delay)"
                    } else {
                        "Invalid password or corrupted vault.key"
                    }
                }
                _uiState.value = UnlockUiState.Error(msg)
            } finally {
                SecureMemory.wipe(passwordChars)
            }
        }
    }

    fun unlockWithBiometrics(
        activity: FragmentActivity,
        onUnlocked: () -> Unit
    ) {
        biometricUnlockManager.promptBiometricUnlock(
            activity = activity,
            onSuccess = {
                _uiState.value = UnlockUiState.Success
                onUnlocked()
            },
            onError = { err ->
                _uiState.value = UnlockUiState.Error(err)
            }
        )
    }

    fun enableBiometrics(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            vaultSession.withMasterKey { masterKey ->
                biometricUnlockManager.promptEnableBiometrics(activity, masterKey, onSuccess, onError)
            }
        } catch (e: Exception) {
            onError("Vault must be unlocked to enable biometrics")
        }
    }
}
