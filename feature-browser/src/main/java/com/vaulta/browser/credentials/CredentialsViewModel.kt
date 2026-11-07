package com.vaulta.browser.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulta.crypto.PasswordAnalysis
import com.vaulta.crypto.PasswordGenerator
import com.vaulta.crypto.PasswordStrength
import com.vaulta.crypto.TotpGenerator
import com.vaulta.storage.credentials.CredentialCategory
import com.vaulta.storage.credentials.CredentialDao
import com.vaulta.storage.credentials.PasswordHistoryEntry
import com.vaulta.storage.credentials.VaultCredential
import com.vaulta.storage.credentials.toDomain
import com.vaulta.storage.credentials.toEntity
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.vault.clipboard.ClipboardAutoPurgeManager
import com.vaulta.vault.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class TotpItemState(
    val code: String,
    val formattedCode: String,
    val remainingSeconds: Int,
    val progress: Float
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CredentialsViewModel @Inject constructor(
    private val credentialDao: CredentialDao,
    private val encryptedIndex: EncryptedIndex,
    private val vaultSession: VaultSession,
    private val clipboardAutoPurgeManager: ClipboardAutoPurgeManager,
    private val credentialArchive: CredentialArchive
) : ViewModel() {

    private val credentialWriteMutex = Mutex()

    private val _selectedCategory = MutableStateFlow<CredentialCategory?>(null)
    val selectedCategory: StateFlow<CredentialCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _userMessage = Channel<String>(Channel.BUFFERED)
    val userMessage: Flow<String> = _userMessage.receiveAsFlow()

    private val _totpRemainingSeconds = MutableStateFlow(30)
    val totpRemainingSeconds: StateFlow<Int> = _totpRemainingSeconds.asStateFlow()

    private val _totpStateMap = MutableStateFlow<Map<String, TotpItemState>>(emptyMap())
    val totpStateMap: StateFlow<Map<String, TotpItemState>> = _totpStateMap.asStateFlow()

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
                val secs = TotpGenerator.getRemainingSeconds()
                _totpRemainingSeconds.value = secs
                val currentCreds = credentials.value
                val newMap = mutableMapOf<String, TotpItemState>()
                for (cred in currentCreds) {
                    if (cred.totpSecret.isNotBlank()) {
                        val code = TotpGenerator.generateTotp(cred.totpSecret)
                        val formatted = if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
                        val progress = secs / 30f
                        newMap[cred.id] = TotpItemState(
                            code = code,
                            formattedCode = formatted,
                            remainingSeconds = secs,
                            progress = progress
                        )
                    }
                }
                _totpStateMap.value = newMap
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
            credentialWriteMutex.withLock {
                val previous = credentialDao.getCredentialById(credential.id)
                try {
                    val existing = previous?.toDomain()
                    val updatedHistory = if (existing != null && existing.password.isNotBlank() && existing.password != credential.password) {
                        listOf(PasswordHistoryEntry(System.currentTimeMillis(), existing.password)) + existing.passwordHistory
                    } else credential.passwordHistory
                    val finalCred = credential.copy(modifiedAt = System.currentTimeMillis(), passwordHistory = updatedHistory)
                    credentialDao.insertCredential(finalCred.toEntity())
                    saveEncryptedSnapshot()
                    _userMessage.trySend("Credential saved successfully")
                } catch (_: Exception) {
                    runCatching {
                        if (previous == null) credentialDao.getCredentialById(credential.id)?.let { credentialDao.deleteCredential(it) }
                        else credentialDao.insertCredential(previous)
                    }
                    _userMessage.trySend("Could not save password securely. The previous saved version was kept. Try again.")
                }
            }
        }
    }

    fun deleteCredential(credential: VaultCredential) {
        viewModelScope.launch(Dispatchers.IO) {
            credentialWriteMutex.withLock {
                val previous = credentialDao.getCredentialById(credential.id) ?: return@withLock
                try {
                    credentialDao.deleteCredential(previous)
                    saveEncryptedSnapshot()
                    _userMessage.trySend("Credential deleted")
                } catch (_: Exception) {
                    runCatching { credentialDao.insertCredential(previous) }
                    _userMessage.trySend("Could not save the deletion securely. The password was kept. Try again.")
                }
            }
        }
    }

    fun copyToClipboard(label: String, secretText: String) {
        if (secretText.isBlank()) return
        clipboardAutoPurgeManager.copySensitiveText(label, secretText)
        _userMessage.trySend("$label copied! Auto-clears in 30s.")
    }

    fun analyzePassword(password: String): PasswordAnalysis {
        return PasswordGenerator.analyzePassword(password)
    }

    fun generatePassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true,
        avoidAmbiguous: Boolean = false
    ): String {
        return PasswordGenerator.generatePassword(
            length = length,
            includeUpper = includeUpper,
            includeLower = includeLower,
            includeDigits = includeDigits,
            includeSymbols = includeSymbols,
            avoidAmbiguous = avoidAmbiguous
        )
    }

    fun generatePassphrase(wordCount: Int = 4, separator: String = "-"): String {
        return PasswordGenerator.generatePassphrase(wordCount, separator)
    }

    fun calculateStrength(password: String): PasswordStrength {
        return PasswordGenerator.calculateStrength(password)
    }

    fun generateTotpCode(totpSecret: String): String {
        return TotpGenerator.generateTotp(totpSecret)
    }

    fun exportEncryptedArchive(password: CharArray, onReady: (ByteArray?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val creds = credentialDao.getAllCredentialsList().map { it.toDomain() }
                val archive = credentialArchive.encrypt(creds, password)
                withContext(Dispatchers.Main) { onReady(archive) }
            } catch (_: Exception) {
                password.fill('\u0000')
                _userMessage.trySend("Could not create encrypted archive")
                withContext(Dispatchers.Main) { onReady(null) }
            }
        }
    }

    fun importEncryptedArchive(archive: ByteArray, password: CharArray) {
        viewModelScope.launch(Dispatchers.IO) {
            val imported = try {
                credentialArchive.decrypt(archive, password)
            } catch (_: Exception) {
                password.fill('\u0000')
                archive.fill(0)
                _userMessage.trySend("Could not open archive. Check the file and password.")
                return@launch
            }
            credentialWriteMutex.withLock {
                val previous = credentialDao.getAllCredentialsList()
                try {
                    credentialDao.insertCredentials(imported.map { it.toEntity() })
                    saveEncryptedSnapshot()
                    _userMessage.trySend("Imported ${imported.size} encrypted Vaulta credentials")
                } catch (_: Exception) {
                    runCatching {
                        credentialDao.deleteAll()
                        credentialDao.insertCredentials(previous)
                    }
                    _userMessage.trySend("Import could not be saved securely; existing passwords were kept. Try again.")
                } finally {
                    archive.fill(0)
                }
            }
        }
    }

    private suspend fun saveEncryptedSnapshot() {
        vaultSession.withMasterKey { masterKey ->
            encryptedIndex.saveIndex(masterKey)
        }
    }

    fun reportArchiveReadError() { _userMessage.trySend("Could not read encrypted Vaulta archive") }
}
