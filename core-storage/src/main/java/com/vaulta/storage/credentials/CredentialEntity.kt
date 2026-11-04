package com.vaulta.storage.credentials

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

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
    val isFavorite: Boolean,
    val passwordHistoryJson: String = "[]",
    val customFieldsJson: String = "{}",
    val expiresAt: Long? = null,
    val tagsCsv: String = "",
    val colorTag: Int? = null,
    val templateType: String = "LOGIN"
)

fun CredentialEntity.toDomain(): VaultCredential {
    val historyList = mutableListOf<PasswordHistoryEntry>()
    try {
        val arr = JSONArray(passwordHistoryJson)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            historyList.add(
                PasswordHistoryEntry(
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    password = obj.optString("password", "")
                )
            )
        }
    } catch (_: Exception) {}

    val customMap = mutableMapOf<String, String>()
    try {
        val obj = JSONObject(customFieldsJson)
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            customMap[key] = obj.optString(key, "")
        }
    } catch (_: Exception) {}

    val tagList = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

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
        isFavorite = isFavorite,
        passwordHistory = historyList,
        customFields = customMap,
        expiresAt = expiresAt,
        tags = tagList,
        colorTag = colorTag,
        templateType = try { CredentialTemplate.valueOf(templateType) } catch (_: Exception) { CredentialTemplate.LOGIN }
    )
}

fun VaultCredential.toEntity(): CredentialEntity {
    val historyArr = JSONArray()
    for (entry in passwordHistory) {
        val obj = JSONObject().apply {
            put("timestamp", entry.timestamp)
            put("password", entry.password)
        }
        historyArr.put(obj)
    }

    val customObj = JSONObject()
    for ((key, value) in customFields) {
        customObj.put(key, value)
    }

    val tagsString = tags.joinToString(",")

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
        isFavorite = isFavorite,
        passwordHistoryJson = historyArr.toString(),
        customFieldsJson = customObj.toString(),
        expiresAt = expiresAt,
        tagsCsv = tagsString,
        colorTag = colorTag,
        templateType = templateType.name
    )
}
