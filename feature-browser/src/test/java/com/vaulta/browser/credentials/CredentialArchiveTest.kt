package com.vaulta.browser.credentials

import com.google.common.truth.Truth.assertThat
import com.vaulta.crypto.KeyDerivationImpl
import com.vaulta.crypto.SecureRandomProvider
import com.vaulta.crypto.StreamingEncryptionImpl
import com.vaulta.storage.credentials.CredentialCategory
import com.vaulta.storage.credentials.PasswordHistoryEntry
import com.vaulta.storage.credentials.VaultCredential
import org.junit.Assert.assertThrows
import org.junit.Test

class CredentialArchiveTest {
    private val random = object : SecureRandomProvider {
        private val secureRandom = java.security.SecureRandom()
        override fun nextBytes(size: Int): ByteArray = ByteArray(size).also(secureRandom::nextBytes)
    }
    private val archive = CredentialArchive(KeyDerivationImpl(random), StreamingEncryptionImpl(random))

    @Test
    fun encryptThenDecrypt_restoresVaultaCredential_withoutPlaintextLeak() {
        val expected = VaultCredential(
            id = "qa-credential", category = CredentialCategory.LOGIN, title = "QA account",
            username = "qa-user@example.test", password = "unique-secret-value", url = "https://example.test",
            notes = "private note", passwordHistory = listOf(PasswordHistoryEntry(42, "older-secret")),
            customFields = mapOf("recovery" to "recovery-secret"), tags = listOf("qa")
        )
        val encrypted = archive.encrypt(listOf(expected), "a-strong-test-password".toCharArray())
        val bytesAsText = encrypted.toString(Charsets.ISO_8859_1)
        assertThat(bytesAsText).doesNotContain(expected.password)
        assertThat(bytesAsText).doesNotContain(expected.username)
        assertThat(archive.decrypt(encrypted, "a-strong-test-password".toCharArray())).containsExactly(expected)
    }

    @Test
    fun decrypt_rejectsWrongPasswordAndTampering() {
        val encrypted = archive.encrypt(listOf(VaultCredential(title = "QA")), "a-strong-test-password".toCharArray())
        assertThrows(Exception::class.java) { archive.decrypt(encrypted.copyOf(), "another-strong-password".toCharArray()) }
        val tampered = encrypted.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertThrows(Exception::class.java) { archive.decrypt(tampered, "a-strong-test-password".toCharArray()) }
    }

    @Test
    fun encrypt_requiresStrongArchivePassword() {
        assertThrows(IllegalArgumentException::class.java) {
            archive.encrypt(emptyList(), "short".toCharArray())
        }
    }
}
