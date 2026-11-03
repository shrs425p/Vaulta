package com.vaulta.crypto

import java.security.SecureRandom
import java.util.Arrays

/**
 * Utilities for secure memory management, ensuring keys and credentials
 * are zeroized immediately after use.
 */
object SecureMemory {

    private val secureRandom = SecureRandom()

    fun wipe(array: ByteArray) {
        Arrays.fill(array, 0.toByte())
    }

    fun wipe(array: CharArray) {
        Arrays.fill(array, '\u0000')
    }

    inline fun <T> useKey(key: ByteArray, block: (ByteArray) -> T): T {
        return try {
            block(key)
        } finally {
            wipe(key)
        }
    }

    inline fun <T> usePassword(password: CharArray, block: (CharArray) -> T): T {
        return try {
            block(password)
        } finally {
            wipe(password)
        }
    }

    fun generateRandomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }
}
