package com.vaulta.crypto

/**
 * Abstracts AES key wrapping / unwrapping operations.
 */
interface KeyWrapping {

    /**
     * Wraps (encrypts) a key using a wrapping key.
     *
     * @param keyToWrap The plaintext key material to protect.
     * @param wrappingKey The key used for wrapping (e.g., derived from password via Argon2id).
     * @return The wrapped (encrypted) key bytes.
     */
    fun wrap(keyToWrap: ByteArray, wrappingKey: ByteArray): ByteArray

    /**
     * Unwraps (decrypts) a previously wrapped key.
     *
     * @param wrappedKey The wrapped key bytes.
     * @param wrappingKey The key used for unwrapping.
     * @return The original plaintext key material.
     * @throws KeyUnwrapException if the wrapping key is incorrect or data is corrupted.
     */
    fun unwrap(wrappedKey: ByteArray, wrappingKey: ByteArray): ByteArray
}
