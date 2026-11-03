package com.vaulta.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SecureMemoryTest {

    @Test
    fun wipe_zeroesByteArray() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5, 99, 127)
        SecureMemory.wipe(bytes)

        for (b in bytes) {
            assertThat(b).isEqualTo(0.toByte())
        }
    }

    @Test
    fun wipe_zeroesCharArray() {
        val chars = "SecretPassword".toCharArray()
        SecureMemory.wipe(chars)

        for (c in chars) {
            assertThat(c).isEqualTo('\u0000')
        }
    }

    @Test
    fun useKey_zeroesKeyAfterExecution() {
        val key = byteArrayOf(10, 20, 30, 40)
        var capturedValue = 0

        SecureMemory.useKey(key) {
            capturedValue = it[0].toInt()
        }

        assertThat(capturedValue).isEqualTo(10)
        assertThat(key[0]).isEqualTo(0.toByte())
    }
}
