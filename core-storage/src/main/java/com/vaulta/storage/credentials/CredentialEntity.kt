package com.vaulta.storage.credentials

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_credentials")
data class CredentialEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val totpSecret: String,
    val cardNumber: String,
    val cardholderName: String,
    val cardExpiry: String,
    val cardCvv: String,
    val notes: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val isFavorite: Boolean
)

fun CredentialEntity.toDomain(): VaultCredential {
    return VaultCredential(
        id = id,
        category = try { CredentialCategory.valueOf(category) } catch (_: Exception) { CredentialCategory.LOGIN },
        title = title,
        username = username,
        password = password,
        url = url,
        totpSecret = totpSecret,
        cardNumber = cardNumber,
        cardholderName = cardholderName,
        cardExpiry = cardExpiry,
        cardCvv = cardCvv,
        notes = notes,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        isFavorite = isFavorite
    )
}

fun VaultCredential.toEntity(): CredentialEntity {
    return CredentialEntity(
        id = id,
        category = category.name,
        title = title,
        username = username,
        password = password,
        url = url,
        totpSecret = totpSecret,
        cardNumber = cardNumber,
        cardholderName = cardholderName,
        cardExpiry = cardExpiry,
        cardCvv = cardCvv,
        notes = notes,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        isFavorite = isFavorite
    )
}
