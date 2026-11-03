package com.vaulta.browser.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulta.crypto.PasswordGenerator
import com.vaulta.crypto.PasswordStrength
import com.vaulta.crypto.TotpGenerator
import com.vaulta.storage.credentials.CredentialCategory
import com.vaulta.storage.credentials.CredentialDao
import com.vaulta.storage.credentials.VaultCredential
import com.vaulta.storage.credentials.toDomain
import com.vaulta.storage.credentials.toEntity
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.vault.clipboard.ClipboardAutoPurgeManager
import com.vaulta.vault.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CredentialsViewModel @Inject constructor(
    private val credentialDao: CredentialDao,
    private val encryptedIndex: EncryptedIndex,
    private val vaultSession: VaultSession,
    private val clipboardAutoPurgeManager: ClipboardAutoPurgeManager
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<CredentialCategory?>(null)
    val selectedCategory: StateFlow<CredentialCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _totpRemainingSeconds = MutableStateFlow(30)
    val totpRemainingSeconds: StateFlow<Int> = _totpRemainingSeconds.asStateFlow()

    val credentials: StateFlow<List<VaultCredential>> = combine(
        _selectedCategory,
        _searchQuery
    ) { category, query -> Pair(category, query) }
        .flatMapLatest { (category, query) ->
            if (query.isNotBlank()) {
                credentialDao.searchCredentials(query)
            } else if (category != null) {
                credentialDao.getCredentialsByCategory(category.name)
            } else {
                credentialDao.getAllCredentials()
            }
        }
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                _totpRemainingSeconds.value = TotpGenerator.getRemainingSeconds()
                delay(1000)
            }
        }
    }

    fun selectCategory(category: CredentialCategory?) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveCredential(credential: VaultCredential) {
        viewModelScope.launch(Dispatchers.IO) {
            credentialDao.insertCredential(credential.toEntity())
            try {
                vaultSession.withMasterKey { masterKey ->
                    encryptedIndex.saveIndex(masterKey)
                }
            } catch (_: Exception) {}
            _userMessage.value = "Credential saved successfully"
        }
    }

    fun deleteCredential(credential: VaultCredential) {
        viewModelScope.launch(Dispatchers.IO) {
            credentialDao.deleteCredential(credential.toEntity())
            try {
                vaultSession.withMasterKey { masterKey ->
                    encryptedIndex.saveIndex(masterKey)
                }
            } catch (_: Exception) {}
            _userMessage.value = "Credential deleted"
        }
    }

    fun copyToClipboard(label: String, secretText: String) {
        if (secretText.isBlank()) return
        clipboardAutoPurgeManager.copySensitiveText(label, secretText)
        _userMessage.value = "$label copied! Auto-clears in 30s."
    }

    fun generatePassword(length: Int = 16): String {
        return PasswordGenerator.generatePassword(length)
    }

    fun generatePassphrase(wordCount: Int = 4): String {
        return PasswordGenerator.generatePassphrase(wordCount)
    }

    fun calculateStrength(password: String): PasswordStrength {
        return PasswordGenerator.calculateStrength(password)
    }

    fun generateTotpCode(totpSecret: String): String {
        return TotpGenerator.generateTotp(totpSecret)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
