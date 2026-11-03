package com.vaulta.crypto

/**
 * Abstracts secure random byte generation for testability.
 */
interface SecureRandomProvider {

    /**
     * Generates [size] cryptographically secure random bytes.
     */
    fun nextBytes(size: Int): ByteArray
}
