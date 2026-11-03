package com.vaulta.crypto

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyWrappingImpl @Inject constructor(
    private val randomProvider: SecureRandomProvider
) : KeyWrapping {

    override fun wrap(keyToWrap: ByteArray, wrappingKey: ByteArray): ByteArray {
        val nonce = randomProvider.nextBytes(CryptoConstants.NONCE_SIZE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(wrappingKey, "AES")
        val gcmSpec = GCMParameterSpec(CryptoConstants.TAG_SIZE_BYTES * 8, nonce)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertextAndTag = cipher.doFinal(keyToWrap)

        // Concatenate nonce + ciphertext + tag
        val result = ByteArray(nonce.size + ciphertextAndTag.size)
        System.arraycopy(nonce, 0, result, 0, nonce.size)
        System.arraycopy(ciphertextAndTag, 0, result, nonce.size, ciphertextAndTag.size)
        return result
    }

    override fun unwrap(wrappedKey: ByteArray, wrappingKey: ByteArray): ByteArray {
        if (wrappedKey.size < CryptoConstants.NONCE_SIZE_BYTES + CryptoConstants.TAG_SIZE_BYTES) {
            throw KeyUnwrapException("Wrapped key data is too short")
        }

        val nonce = ByteArray(CryptoConstants.NONCE_SIZE_BYTES)
        System.arraycopy(wrappedKey, 0, nonce, 0, CryptoConstants.NONCE_SIZE_BYTES)

        val ciphertextLen = wrappedKey.size - CryptoConstants.NONCE_SIZE_BYTES
        val ciphertextAndTag = ByteArray(ciphertextLen)
        System.arraycopy(wrappedKey, CryptoConstants.NONCE_SIZE_BYTES, ciphertextAndTag, 0, ciphertextLen)

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secretKey = SecretKeySpec(wrappingKey, "AES")
            val gcmSpec = GCMParameterSpec(CryptoConstants.TAG_SIZE_BYTES * 8, nonce)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            return cipher.doFinal(ciphertextAndTag)
        } catch (e: GeneralSecurityException) {
            throw KeyUnwrapException("Failed to unwrap key. Wrong password/recovery code or corrupted data.", e)
        }
    }
}
