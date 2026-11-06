package com.vaulta.settings

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulta.crypto.SecureMemory
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.storage.index.VaultStats
import com.vaulta.vault.sync.CloudStorageStats
import com.vaulta.vault.sync.SyncProvider
import com.vaulta.vault.VaultOperations
import com.vaulta.vault.VaultPasswordPolicy
import com.vaulta.vault.biometric.BiometricUnlockManager
import com.vaulta.vault.preferences.AppPreferencesManager
import com.vaulta.vault.preferences.AppThemeMode
import com.vaulta.vault.preferences.AppThemeStyle
import com.vaulta.vault.preferences.KeyFilePreferenceManager
import com.vaulta.vault.session.AutoLockManager
import com.vaulta.vault.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsUiState {
    data object Idle : SettingsUiState
    data object Processing : SettingsUiState
    data class Success(val message: String) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val vaultOperations: VaultOperations,
    private val vaultSession: VaultSession,
    private val autoLockManager: AutoLockManager,
    private val syncProvider: SyncProvider,
    val biometricUnlockManager: BiometricUnlockManager,
    private val keyFilePreferenceManager: KeyFilePreferenceManager,
    private val appPreferencesManager: AppPreferencesManager,
    private val encryptedIndex: EncryptedIndex
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _isSyncAvailable = MutableStateFlow(syncProvider.isAvailable)
    val isSyncAvailable: StateFlow<Boolean> = _isSyncAvailable.asStateFlow()
    val isSyncAvailableValue: Boolean get() = _isSyncAvailable.value

    private val _signedInEmail = MutableStateFlow<String?>(syncProvider.getSignedInEmail())
    val signedInEmail: StateFlow<String?> = _signedInEmail.asStateFlow()

    val syncProviderName: String
        get() = syncProvider.providerName

    val isOfflineEdition: Boolean
        get() = syncProvider.providerName == "Offline (Air-Gapped)"

    val autoLockTimeoutMinutes: StateFlow<Int> = appPreferencesManager.autoLockTimeoutMinutes

    private val _isBiometricEnabled = MutableStateFlow(biometricUnlockManager.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    val isScreenProtectionEnabled: StateFlow<Boolean> = appPreferencesManager.isScreenProtectionEnabled
    val deleteOriginalAfterImport: StateFlow<Boolean> = appPreferencesManager.deleteOriginalAfterImport
    val defaultSyncPolicy: StateFlow<com.vaulta.vault.preferences.DefaultSyncPolicy> = appPreferencesManager.defaultSyncPolicy
    val isStealthSyncEnabled: StateFlow<Boolean> = appPreferencesManager.isStealthSyncEnabled
    val themeMode: StateFlow<AppThemeMode> = appPreferencesManager.themeMode
    val themeStyle: StateFlow<AppThemeStyle> = appPreferencesManager.themeStyle

    private val _vaultStats = MutableStateFlow<VaultStats?>(null)
    val vaultStats: StateFlow<VaultStats?> = _vaultStats.asStateFlow()

    private val _cloudStorageStats = MutableStateFlow<CloudStorageStats?>(null)
    val cloudStorageStats: StateFlow<CloudStorageStats?> = _cloudStorageStats.asStateFlow()

    private val _isFetchingCloudStats = MutableStateFlow(false)
    val isFetchingCloudStats: StateFlow<Boolean> = _isFetchingCloudStats.asStateFlow()

    init {
        refreshStats()
        fetchCloudStats()
    }

    fun refreshStats() {
        _signedInEmail.value = syncProvider.getSignedInEmail()
        viewModelScope.launch {
            try {
                _vaultStats.value = encryptedIndex.getVaultStats()
            } catch (_: Exception) {}
        }
    }

    fun fetchCloudStats() {
        if (!syncProvider.isAvailable) return
        _isFetchingCloudStats.value = true
        viewModelScope.launch {
            try {
                val result = syncProvider.getCloudStorageStats()
                if (result.isSuccess) {
                    _cloudStorageStats.value = result.getOrNull()
                }
            } catch (_: Exception) {
            } finally {
                _isFetchingCloudStats.value = false
            }
        }
    }

    fun wipeCloudStorage() {
        if (!syncProvider.isAvailable) return
        _uiState.value = SettingsUiState.Processing
        viewModelScope.launch {
            val result = syncProvider.wipeCloudStorage()
            if (result.isSuccess) {
                _cloudStorageStats.value = null
                _uiState.value = SettingsUiState.Success("Cloud backup wiped cleanly from Google Drive")
                fetchCloudStats()
            } else {
                _uiState.value = SettingsUiState.Error(result.exceptionOrNull()?.message ?: "Failed wiping cloud storage")
            }
        }
    }

    fun deleteRemoteBlob(fileId: String) {
        if (!syncProvider.isAvailable) return
        viewModelScope.launch {
            val result = syncProvider.deleteRemoteBlobById(fileId)
            if (result.isSuccess) {
                _uiState.value = SettingsUiState.Success("File deleted from cloud storage")
                fetchCloudStats()
            } else {
                _uiState.value = SettingsUiState.Error("Failed deleting file from cloud")
            }
        }
    }

    fun setScreenProtectionEnabled(enabled: Boolean) {
        appPreferencesManager.setScreenProtectionEnabled(enabled)
    }

    fun setDeleteOriginalAfterImport(enabled: Boolean) {
        appPreferencesManager.setDeleteOriginalAfterImport(enabled)
    }

    fun setDefaultSyncPolicy(policy: com.vaulta.vault.preferences.DefaultSyncPolicy) {
        appPreferencesManager.setDefaultSyncPolicy(policy)
    }

    fun setStealthSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Processing
            val switched = syncProvider.switchStorageSpace(enabled)
            if (switched) {
                appPreferencesManager.setIsStealthSyncEnabled(enabled)
                val msg = if (enabled) {
                    "Stealth Cloud Backup enabled! Backups moved to Google Drive's hidden appDataFolder."
                } else {
                    "Visible Cloud Backup enabled! Backups moved to 'Vaulta Backups'."
                }
                _uiState.value = SettingsUiState.Success(msg)
                syncNow()
            } else {
                _uiState.value = SettingsUiState.Error("Failed switching storage space on Google Drive")
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        appPreferencesManager.setThemeMode(mode)
    }

    fun setThemeStyle(style: AppThemeStyle) {
        appPreferencesManager.setThemeStyle(style)
    }

    fun getSavedKeyFileUri(): Uri? = keyFilePreferenceManager.getSavedKeyFileUri()

    fun getKeyFileName(uri: Uri): String = keyFilePreferenceManager.getFileName(uri)

    fun saveKeyFileUri(uri: Uri) = keyFilePreferenceManager.saveKeyFileUri(uri)

    fun canAuthenticateBiometrics(): Boolean = biometricUnlockManager.canAuthenticate()

    fun refreshBiometricState() {
        _isBiometricEnabled.value = biometricUnlockManager.isBiometricEnabled()
    }

    fun enableBiometrics(activity: FragmentActivity, onResult: (Boolean, String?) -> Unit) {
        try {
            vaultSession.withMasterKey { masterKey ->
                biometricUnlockManager.promptEnableBiometrics(
                    activity = activity,
                    masterKey = masterKey,
                    onSuccess = {
                        _isBiometricEnabled.value = true
                        onResult(true, null)
                    },
                    onError = { error ->
                        _isBiometricEnabled.value = false
                        onResult(false, error)
                    }
                )
            }
        } catch (e: Exception) {
            onResult(false, "Vault must be unlocked to enable biometrics")
        }
    }

    fun disableBiometrics() {
        biometricUnlockManager.disableBiometrics()
        _isBiometricEnabled.value = false
    }

    fun setAutoLockTimeout(minutes: Int) {
        appPreferencesManager.setAutoLockTimeoutMinutes(minutes)
        autoLockManager.setTimeoutMs(minutes * 60 * 1000L)
    }

    fun changePassword(
        currentPassword: CharArray,
        newPassword: CharArray,
        keyFileUri: Uri
    ) {
        if (!VaultPasswordPolicy.accepts(newPassword)) {
            _uiState.value = SettingsUiState.Error("New password must be between ${VaultPasswordPolicy.MIN_LENGTH} and ${VaultPasswordPolicy.MAX_LENGTH} characters")
            SecureMemory.wipe(currentPassword)
            SecureMemory.wipe(newPassword)
            return
        }

        _uiState.value = SettingsUiState.Processing
        viewModelScope.launch {
            try {
                vaultOperations.changePassword(currentPassword, newPassword, keyFileUri)
                _uiState.value = SettingsUiState.Success("Password changed successfully")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("Failed to change password: ${e.message}")
            }
        }
    }

    fun getSignInIntent(context: android.content.Context): android.content.Intent? {
        return syncProvider.getSignInIntent(context)
    }

    fun handleSignInResult(data: android.content.Intent?) {
        val result = syncProvider.handleSignInResult(data)
        _isSyncAvailable.value = syncProvider.isAvailable
        _signedInEmail.value = syncProvider.getSignedInEmail()
        if (result.isSuccess) {
            _uiState.value = SettingsUiState.Success("Signed in as ${result.getOrNull()}")
            syncNow()
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Sign in failed"
            _uiState.value = SettingsUiState.Error(errorMsg)
        }
    }

    fun signOut() {
        syncProvider.signOut()
        _isSyncAvailable.value = syncProvider.isAvailable
        _signedInEmail.value = null
        _cloudStorageStats.value = null
        _uiState.value = SettingsUiState.Success("Signed out of Google Drive")
    }

    fun syncNow() {
        _isSyncAvailable.value = syncProvider.isAvailable
        _signedInEmail.value = syncProvider.getSignedInEmail()
        if (!syncProvider.isAvailable) {
            _uiState.value = SettingsUiState.Error("Google Drive is not signed in. Tap Sign In first.")
            return
        }
        _uiState.value = SettingsUiState.Processing
        viewModelScope.launch {
            val result = syncProvider.sync()
            _isSyncAvailable.value = syncProvider.isAvailable
            _signedInEmail.value = syncProvider.getSignedInEmail()
            if (result.isSuccess) {
                _uiState.value = SettingsUiState.Success("Cloud synchronization completed")
                fetchCloudStats()
            } else {
                _uiState.value = SettingsUiState.Error(
                    result.exceptionOrNull()?.message ?: "Sync failed"
                )
            }
        }
    }

    fun lockVault() {
        vaultSession.lock()
    }

    fun clearState() {
        _uiState.value = SettingsUiState.Idle
    }
}
