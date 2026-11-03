package com.vaulta.crypto

import com.google.common.truth.Truth.assertThat
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import org.junit.Before
import org.junit.Test

class KeyDerivationTest {

    private lateinit var keyDerivation: KeyDerivation
    private lateinit var randomProvider: SecureRandomProvider

    @Before
    fun setUp() {
        randomProvider = SecureRandomProviderImpl()
        keyDerivation = KeyDerivationImpl(randomProvider)
    }

    @Test
    fun deriveKey_producesConsistentOutput_forSameInputs() {
        val password = "StrongPassword123!".toCharArray()
        val salt = "01234567890123456789012345678901".toByteArray()
        val params = Argon2Params(iterations = 1)

        val key1 = keyDerivation.deriveKey(password, salt, params)
        val key2 = keyDerivation.deriveKey(password, salt, params)

        assertThat(key1).isEqualTo(key2)
        assertThat(key1.size).isEqualTo(CryptoConstants.MASTER_KEY_SIZE_BYTES)
    }

    @Test
    fun deriveKey_producesDifferentOutput_forDifferentPasswords() {
        val salt = "01234567890123456789012345678901".toByteArray()
        val params = Argon2Params(iterations = 1)

        val key1 = keyDerivation.deriveKey("PasswordOne123!".toCharArray(), salt, params)
        val key2 = keyDerivation.deriveKey("PasswordTwo123!".toCharArray(), salt, params)

        assertThat(key1).isNotEqualTo(key2)
    }

    @Test
    fun deriveFileKey_producesUniqueKeys_forDifferentSalts() {
        val masterKey = randomProvider.nextBytes(32)
        val salt1 = randomProvider.nextBytes(32)
        val salt2 = randomProvider.nextBytes(32)

        val fileKey1 = keyDerivation.deriveFileKey(masterKey, salt1)
        val fileKey2 = keyDerivation.deriveFileKey(masterKey, salt2)

        assertThat(fileKey1.size).isEqualTo(32)
        assertThat(fileKey2.size).isEqualTo(32)
        assertThat(fileKey1).isNotEqualTo(fileKey2)
    }

    @Test
    fun generateSalt_returns32Bytes() {
        val salt = keyDerivation.generateSalt()
        assertThat(salt.size).isEqualTo(32)
    }

    @Test
    fun generateMasterKey_returns32Bytes() {
        val masterKey = keyDerivation.generateMasterKey()
        assertThat(masterKey.size).isEqualTo(32)
    }
}
