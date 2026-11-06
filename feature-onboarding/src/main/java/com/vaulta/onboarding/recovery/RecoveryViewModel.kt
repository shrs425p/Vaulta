package com.vaulta.onboarding.recovery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulta.crypto.SecureMemory
import com.vaulta.vault.VaultOperations
import com.vaulta.vault.VaultPasswordPolicy
import com.vaulta.vault.preferences.KeyFilePreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecoveryUiState {
    data object Idle : RecoveryUiState
    data object Recovering : RecoveryUiState
    data object Success : RecoveryUiState
    data class Error(val message: String) : RecoveryUiState
}

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val vaultOperations: VaultOperations,
    private val keyFilePreferenceManager: KeyFilePreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecoveryUiState>(RecoveryUiState.Idle)
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

    fun recoverVault(recoveryCode: String, keyFileUri: Uri, newPasswordChars: CharArray) {
        if (!VaultPasswordPolicy.accepts(newPasswordChars)) {
            _uiState.value = RecoveryUiState.Error("New password must be between ${VaultPasswordPolicy.MIN_LENGTH} and ${VaultPasswordPolicy.MAX_LENGTH} characters")
            SecureMemory.wipe(newPasswordChars)
            return
        }

        _uiState.value = RecoveryUiState.Recovering
        viewModelScope.launch {
            try {
                keyFilePreferenceManager.saveKeyFileUri(keyFileUri)
                vaultOperations.recoverWithCode(recoveryCode, keyFileUri, newPasswordChars)
                _uiState.value = RecoveryUiState.Success
            } catch (e: Exception) {
                _uiState.value = RecoveryUiState.Error(
                    e.message ?: "Failed to recover vault. Invalid recovery code."
                )
            } finally {
                SecureMemory.wipe(newPasswordChars)
            }
        }
    }
}
