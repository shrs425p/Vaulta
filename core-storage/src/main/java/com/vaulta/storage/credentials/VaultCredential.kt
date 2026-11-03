package com.vaulta.storage.credentials

import java.util.UUID

enum class CredentialCategory(val label: String) {
    LOGIN("Logins"),
    CARD("Credit Cards"),
    NOTE("Secure Notes"),
    PASSKEY("Passkeys / API Keys")
}

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
    val isFavorite: Boolean = false
)
