package com.vaulta.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * An in-memory encrypted wrapper for sensitive ByteArrays (e.g. passwords, secret keys).
 *
 * Prevents sensitive plaintext secrets from residing continuously in RAM. The underlying data
 * is encrypted with an ephemeral 256-bit AES-GCM key generated upon construction.
 *
 * Use [use] to briefly decrypt and work with the plaintext bytes; the temporary plaintext buffer
 * is guaranteed to be zeroed immediately after use.
 */
class ProtectedByteArray(plaintext: ByteArray) {

    @PublishedApi
    internal val ephemeralKey: ByteArray = ByteArray(32).apply {
        SecureRandom().nextBytes(this)
    }

    @PublishedApi
    internal val nonce: ByteArray = ByteArray(12).apply {
        SecureRandom().nextBytes(this)
    }

    @PublishedApi
    internal val encryptedData: ByteArray

    init {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(ephemeralKey, "AES"),
            GCMParameterSpec(128, nonce)
        )
        encryptedData = cipher.doFinal(plaintext)
    }

    /**
     * Executes [block] with a temporary decrypted copy of the plaintext bytes.
     * Guarantees that the decrypted buffer is securely wiped upon completion or error.
     */
    inline fun <R> use(block: (ByteArray) -> R): R {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(ephemeralKey, "AES"),
            GCMParameterSpec(128, nonce)
        )
        val plaintext = cipher.doFinal(encryptedData)
        return try {
            block(plaintext)
        } finally {
            SecureMemory.wipe(plaintext)
        }
    }

    /**
     * Securely destroys the ephemeral key and encrypted payload.
     */
    fun destroy() {
        SecureMemory.wipe(ephemeralKey)
        SecureMemory.wipe(nonce)
        SecureMemory.wipe(encryptedData)
    }
}
