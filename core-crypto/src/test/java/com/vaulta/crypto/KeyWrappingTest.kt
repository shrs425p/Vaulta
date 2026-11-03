package com.vaulta.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class KeyWrappingTest {

    private lateinit var keyWrapping: KeyWrapping
    private lateinit var randomProvider: SecureRandomProvider

    @Before
    fun setUp() {
        randomProvider = SecureRandomProviderImpl()
        keyWrapping = KeyWrappingImpl(randomProvider)
    }

    @Test
    fun wrapAndUnwrap_returnsOriginalKey() {
        val masterKey = randomProvider.nextBytes(32)
        val wrappingKey = randomProvider.nextBytes(32)

        val wrapped = keyWrapping.wrap(masterKey, wrappingKey)
        val unwrapped = keyWrapping.unwrap(wrapped, wrappingKey)

        assertThat(unwrapped).isEqualTo(masterKey)
    }

    @Test
    fun unwrap_withWrongKey_throwsKeyUnwrapException() {
        val masterKey = randomProvider.nextBytes(32)
        val correctWrappingKey = randomProvider.nextBytes(32)
        val wrongWrappingKey = randomProvider.nextBytes(32)

        val wrapped = keyWrapping.wrap(masterKey, correctWrappingKey)

        assertThrows(KeyUnwrapException::class.java) {
            keyWrapping.unwrap(wrapped, wrongWrappingKey)
        }
    }

    @Test
    fun wrap_producesDifferentCiphertexts_forSameKey() {
        val masterKey = randomProvider.nextBytes(32)
        val wrappingKey = randomProvider.nextBytes(32)

        val wrapped1 = keyWrapping.wrap(masterKey, wrappingKey)
        val wrapped2 = keyWrapping.wrap(masterKey, wrappingKey)

        // Nonces must be random, so ciphertexts must differ
        assertThat(wrapped1).isNotEqualTo(wrapped2)
    }
}
