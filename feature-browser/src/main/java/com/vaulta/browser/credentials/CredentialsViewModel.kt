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
            val existing = credentialDao.getCredentialById(credential.id)?.toDomain()
            val updatedHistory = if (existing != null && existing.password.isNotBlank() && existing.password != credential.password) {
                val newHistoryEntry = PasswordHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    password = existing.password
                )
                listOf(newHistoryEntry) + existing.passwordHistory
            } else {
                credential.passwordHistory
            }

            val finalCred = credential.copy(
                modifiedAt = System.currentTimeMillis(),
                passwordHistory = updatedHistory
            )

            credentialDao.insertCredential(finalCred.toEntity())
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

    fun importCsvVault(csvText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lines = csvText.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) return@launch

                val header = lines.first().split(",").map { it.trim().trim('"').lowercase() }
                val titleIdx = header.indexOfFirst { it.contains("title") || it.contains("name") }.coerceAtLeast(0)
                val userIdx = header.indexOfFirst { it.contains("user") || it.contains("login") || it.contains("email") }
                val passIdx = header.indexOfFirst { it.contains("pass") || it.contains("secret") }
                val urlIdx = header.indexOfFirst { it.contains("url") || it.contains("website") || it.contains("link") }
                val notesIdx = header.indexOfFirst { it.contains("note") || it.contains("extra") }
                val totpIdx = header.indexOfFirst { it.contains("totp") || it.contains("otp") || it.contains("2fa") }

                var count = 0
                for (i in 1 until lines.size) {
                    val parts = parseCsvLine(lines[i])
                    if (parts.isEmpty()) continue

                    val title = parts.getOrNull(titleIdx)?.takeIf { it.isNotBlank() } ?: "Imported Item $i"
                    val username = if (userIdx >= 0) parts.getOrNull(userIdx).orEmpty() else ""
                    val password = if (passIdx >= 0) parts.getOrNull(passIdx).orEmpty() else ""
                    val url = if (urlIdx >= 0) parts.getOrNull(urlIdx).orEmpty() else ""
                    val notes = if (notesIdx >= 0) parts.getOrNull(notesIdx).orEmpty() else ""
                    val totpSecret = if (totpIdx >= 0) parts.getOrNull(totpIdx).orEmpty() else ""

                    val cred = VaultCredential(
                        title = title,
                        username = username,
                        password = password,
                        url = url,
                        notes = notes,
                        totpSecret = totpSecret
                    )
                    credentialDao.insertCredential(cred.toEntity())
                    count++
                }

                vaultSession.withMasterKey { masterKey ->
                    encryptedIndex.saveIndex(masterKey)
                }
                _userMessage.value = "Imported $count credentials into vault"
            } catch (e: Exception) {
                _userMessage.value = "CSV Import error: ${e.message}"
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.clear()
            } else {
                current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}
