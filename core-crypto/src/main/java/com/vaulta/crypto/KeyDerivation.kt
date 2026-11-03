package com.vaulta.crypto

/**
 * Abstracts key derivation functions including password derivation and HKDF file key derivation.
 */
interface KeyDerivation {

    /**
     * Derives a 256-bit (32-byte) key from a password and salt using PBKDF2/Argon2.
     *
     * @param password The user's password as a [CharArray]. Caller is responsible for wiping.
     * @param salt A random 32-byte salt.
     * @param params Argon2 / KDF tuning parameters.
     * @return A 32-byte derived key.
     */
    fun deriveKey(password: CharArray, salt: ByteArray, params: Argon2Params): ByteArray

    /**
     * Derives a unique 256-bit file key from the master key and per-file salt using HKDF-SHA256.
     *
     * @param masterKey The 256-bit vault master key.
     * @param fileSalt The unique 32-byte per-file salt.
     * @return A 32-byte file key.
     */
    fun deriveFileKey(masterKey: ByteArray, fileSalt: ByteArray): ByteArray

    /**
     * Generates a 32-byte cryptographically secure random salt.
     */
    fun generateSalt(): ByteArray

    /**
     * Generates a 32-byte cryptographically secure random master key.
     */
    fun generateMasterKey(): ByteArray
}
