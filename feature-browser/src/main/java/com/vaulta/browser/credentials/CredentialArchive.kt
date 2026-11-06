package com.vaulta.browser.credentials

import com.vaulta.crypto.Argon2Params
import com.vaulta.crypto.KeyDerivation
import com.vaulta.crypto.StreamingEncryption
import com.vaulta.storage.credentials.CredentialCategory
import com.vaulta.storage.credentials.CredentialTemplate
import com.vaulta.storage.credentials.PasswordHistoryEntry
import com.vaulta.storage.credentials.VaultCredential
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Vaulta-only password archive. The header reveals only format and a random KDF salt. */
@Singleton
class CredentialArchive @Inject constructor(
    private val keyDerivation: KeyDerivation,
    private val encryption: StreamingEncryption
) {
    companion object {
        private val MAGIC = "VAULTA-CREDS-1\n".toByteArray(Charsets.US_ASCII)
        private const val MAX_ARCHIVE_BYTES = 10 * 1024 * 1024
        private const val MAX_PLAINTEXT_BYTES = 9 * 1024 * 1024
        private const val MAX_CREDENTIALS = 10_000
        private const val MAX_FIELD_CHARS = 1_000_000
        private val PARAMS = Argon2Params(memoryCostKb = 65536, iterations = 3, parallelism = 4)
    }

    fun encrypt(credentials: List<VaultCredential>, password: CharArray): ByteArray {
        require(password.size >= 12) { "Use an archive password with at least 12 characters" }
        require(credentials.size <= MAX_CREDENTIALS) { "Too many credentials" }
        val plaintext = serialize(credentials)
        require(plaintext.size <= MAX_PLAINTEXT_BYTES) { "Archive is too large" }
        val salt = keyDerivation.generateSalt()
        var key: ByteArray? = null
        return try {
            key = keyDerivation.deriveKey(password, salt, PARAMS)
            val output = Buffer()
            output.write(MAGIC).write(salt)
            encryption.encryptStream(Buffer().write(plaintext), output, key)
            output.readByteArray()
        } finally {
            plaintext.fill(0)
            salt.fill(0)
            key?.fill(0)
            password.fill('\u0000')
        }
    }

    fun decrypt(archive: ByteArray, password: CharArray): List<VaultCredential> {
        require(archive.size in (MAGIC.size + 32 + 10)..MAX_ARCHIVE_BYTES) { "Invalid Vaulta password archive" }
        val input = Buffer().write(archive)
        val magic = input.readByteArray(MAGIC.size.toLong())
        if (!MessageDigest.isEqual(magic, MAGIC)) throw IOException("This is not a Vaulta password archive")
        val salt = input.readByteArray(32)
        var key: ByteArray? = null
        val plaintext = Buffer()
        return try {
            key = keyDerivation.deriveKey(password, salt, PARAMS)
            encryption.decryptStream(input, plaintext, key)
            if (plaintext.size > MAX_ARCHIVE_BYTES) throw IOException("Archive is too large")
            parse(plaintext.readByteArray())
        } finally {
            salt.fill(0)
            key?.fill(0)
            password.fill('\u0000')
            val remaining = plaintext.readByteArray()
            remaining.fill(0)
        }
    }

    private fun serialize(credentials: List<VaultCredential>): ByteArray {
        val root = JSONObject().put("format", "vaulta-credentials").put("version", 1)
        val entries = JSONArray()
        credentials.forEach { c ->
            val history = JSONArray()
            c.passwordHistory.take(1000).forEach { history.put(JSONObject().put("timestamp", it.timestamp).put("password", it.password)) }
            val custom = JSONObject()
            c.customFields.entries.take(1000).forEach { (k, v) -> custom.put(k, v) }
            val tags = JSONArray()
            c.tags.take(1000).forEach(tags::put)
            entries.put(JSONObject()
                .put("id", c.id).put("category", c.category.name).put("title", c.title)
                .put("username", c.username).put("password", c.password).put("url", c.url)
                .put("totpSecret", c.totpSecret).put("cardNumber", c.cardNumber)
                .put("cardholderName", c.cardholderName).put("cardExpiry", c.cardExpiry)
                .put("cardCvv", c.cardCvv).put("notes", c.notes).put("createdAt", c.createdAt)
                .put("modifiedAt", c.modifiedAt).put("isFavorite", c.isFavorite)
                .put("passwordHistory", history).put("customFields", custom)
                .put("expiresAt", c.expiresAt ?: JSONObject.NULL).put("tags", tags)
                .put("colorTag", c.colorTag ?: JSONObject.NULL).put("templateType", c.templateType.name))
        }
        root.put("credentials", entries)
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    private fun parse(bytes: ByteArray): List<VaultCredential> {
        try {
            val root = JSONObject(String(bytes, Charsets.UTF_8))
            if (root.optString("format") != "vaulta-credentials" || root.optInt("version") != 1) throw IOException("Unsupported Vaulta archive")
            val rows = root.getJSONArray("credentials")
            if (rows.length() > MAX_CREDENTIALS) throw IOException("Too many credentials")
            return (0 until rows.length()).map { i ->
                val o = rows.getJSONObject(i)
                fun str(name: String, fallback: String = ""): String = o.optString(name, fallback).also { if (it.length > MAX_FIELD_CHARS) throw IOException("Invalid archive field") }
                val history = o.optJSONArray("passwordHistory") ?: JSONArray()
                val custom = o.optJSONObject("customFields") ?: JSONObject()
                val customNames = custom.names() ?: JSONArray()
                val tags = o.optJSONArray("tags") ?: JSONArray()
                VaultCredential(
                    id = str("id"), category = enumValueOf<CredentialCategory>(str("category", "LOGIN")),
                    title = str("title"), username = str("username"), password = str("password"), url = str("url"),
                    totpSecret = str("totpSecret"), cardNumber = str("cardNumber"), cardholderName = str("cardholderName"),
                    cardExpiry = str("cardExpiry"), cardCvv = str("cardCvv"), notes = str("notes"),
                    createdAt = o.optLong("createdAt"), modifiedAt = o.optLong("modifiedAt"), isFavorite = o.optBoolean("isFavorite"),
                    passwordHistory = (0 until history.length()).take(1000).map { j -> history.getJSONObject(j).let { PasswordHistoryEntry(it.optLong("timestamp"), it.optString("password")) } },
                    customFields = (0 until customNames.length()).take(1000).associate { j -> customNames.getString(j).let { it to custom.getString(it) } },
                    expiresAt = if (o.isNull("expiresAt")) null else o.optLong("expiresAt"),
                    tags = (0 until tags.length()).take(1000).map(tags::getString),
                    colorTag = if (o.isNull("colorTag")) null else o.optInt("colorTag"),
                    templateType = enumValueOf<CredentialTemplate>(str("templateType", "LOGIN"))
                )
            }
        } finally { bytes.fill(0) }
    }
}
