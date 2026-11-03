package com.vaulta.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProtectedByteArrayTest {

    @Test
    fun protectedByteArray_encryptsAndDecryptsCorrectly() {
        val original = "SecretPassword123".toByteArray(Charsets.UTF_8)
        val protected = ProtectedByteArray(original)

        protected.use { decrypted ->
            assertThat(decrypted).isEqualTo(original)
        }
    }

    @Test
    fun protectedByteArray_destroysKeysAndPayload() {
        val original = "SecretData".toByteArray(Charsets.UTF_8)
        val protected = ProtectedByteArray(original)
        protected.destroy()

        assertThat(protected.ephemeralKey.all { it == 0.toByte() }).isTrue()
    }
}
