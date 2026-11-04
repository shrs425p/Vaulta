package com.vaulta.storage.credentials

import java.util.UUID

enum class CredentialCategory(val label: String) {
    LOGIN("Logins"),
    CARD("Credit Cards"),
    NOTE("Secure Notes"),
    PASSKEY("Passkeys / API Keys")
}

enum class CredentialTemplate(val displayName: String) {
    LOGIN("Standard Login"),
    CREDIT_CARD("Credit Card"),
    SECURE_NOTE("Secure Note"),
    SERVER("Server / SSH"),
    WIFI("Wi-Fi Key")
}

data class PasswordHistoryEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val password: String
)

data class VaultCredential(
    val id: String = UUID.randomUUID().toString(),
    val category: CredentialCategory = CredentialCategory.LOGIN,
    val title: String,
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val totpSecret: String = "",
    val cardNumber: String = "",
    val cardholderName: String = "",
    val cardExpiry: String = "",
    val cardCvv: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val passwordHistory: List<PasswordHistoryEntry> = emptyList(),
    val customFields: Map<String, String> = emptyMap(),
    val expiresAt: Long? = null,
    val tags: List<String> = emptyList(),
    val colorTag: Int? = null,
    val templateType: CredentialTemplate = CredentialTemplate.LOGIN
)
