package com.vaulta.onboarding.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulta.crypto.SecureMemory
import com.vaulta.vault.VaultOperations
import com.vaulta.vault.preferences.KeyFilePreferenceManager
import com.vaulta.vault.clipboard.ClipboardAutoPurgeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CreateVaultUiState {
    data object Idle : CreateVaultUiState
    data object Creating : CreateVaultUiState
    data class Success(val recoveryCode: String) : CreateVaultUiState
    data class Error(val message: String) : CreateVaultUiState
}

@HiltViewModel
class CreateVaultViewModel @Inject constructor(
    private val vaultOperations: VaultOperations,
    private val keyFilePreferenceManager: KeyFilePreferenceManager,
    private val clipboardAutoPurgeManager: ClipboardAutoPurgeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateVaultUiState>(CreateVaultUiState.Idle)
    val uiState: StateFlow<CreateVaultUiState> = _uiState.asStateFlow()

    fun copyRecoveryCode(code: String) {
        clipboardAutoPurgeManager.copySensitiveText("Recovery Code", code, purgeDelayMs = 60_000L)
    }

    fun createVault(passwordChars: CharArray, keyFileUri: Uri) {
        if (passwordChars.size < 8) {
            _uiState.value = CreateVaultUiState.Error("Password must be at least 8 characters long")
            SecureMemory.wipe(passwordChars)
            return
        }

        _uiState.value = CreateVaultUiState.Creating
        viewModelScope.launch {
            try {
                keyFilePreferenceManager.saveKeyFileUri(keyFileUri)
                val result = vaultOperations.createVault(passwordChars, keyFileUri)
                _uiState.value = CreateVaultUiState.Success(result.recoveryCode)
            } catch (e: Exception) {
                _uiState.value = CreateVaultUiState.Error(e.message ?: "Failed to create vault")
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateVaultUiState.Idle
    }
}
