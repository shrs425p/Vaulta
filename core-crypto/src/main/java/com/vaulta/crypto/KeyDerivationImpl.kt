package com.vaulta.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyDerivationImpl @Inject constructor(
    private val randomProvider: SecureRandomProvider
) : KeyDerivation {

    private val isArgon2Supported: Boolean by lazy {
        try {
            Argon2Kt()
            true
        } catch (_: Throwable) {
            false
        }
    }

    override fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        params: Argon2Params
    ): ByteArray {
        if (params.memoryCostKb <= 0) {
            return deriveKeyPbkdf2(
                password = password,
                salt = salt,
                iterations = (params.iterations * 25000).coerceAtLeast(100000)
            )
        }

        if (isArgon2Supported) {
            val argon2Kt = Argon2Kt()
            val byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password))
            val passwordBytes = ByteArray(byteBuffer.remaining())
            byteBuffer.get(passwordBytes)
            // Zero out intermediate byteBuffer
            byteBuffer.clear()
            while (byteBuffer.hasRemaining()) {
                byteBuffer.put(0.toByte())
            }

            return try {
                val result = argon2Kt.hash(
                    mode = Argon2Mode.ARGON2_ID,
                    password = passwordBytes,
                    salt = salt,
                    tCostInIterations = params.iterations,
                    mCostInKibibyte = params.memoryCostKb,
                    parallelism = params.parallelism,
                    hashLengthInBytes = CryptoConstants.MASTER_KEY_SIZE_BYTES
                )
                result.rawHashAsByteArray()
            } finally {
                SecureMemory.wipe(passwordBytes)
            }
        }

        val isAndroid = try {
            android.os.Build.VERSION.SDK_INT > 0
        } catch (_: Throwable) {
            false
        }

        if (isAndroid) {
            throw IllegalStateException("Argon2id native cryptographic library failed to load on this Android device.")
        }

        // Host JVM test environment (desktop) fallback for unit testing when native .so libraries are absent
        return deriveKeyPbkdf2(
            password = password,
            salt = salt,
            iterations = (params.iterations * 25000).coerceAtLeast(100000)
        )
    }

    fun deriveKeyPbkdf2(
        password: CharArray,
        salt: ByteArray,
        iterations: Int
    ): ByteArray {
        val keySpec = PBEKeySpec(
            password,
            salt,
            iterations,
            CryptoConstants.MASTER_KEY_SIZE_BYTES * 8
        )
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derivedKey = keyFactory.generateSecret(keySpec).encoded
        keySpec.clearPassword()
        return derivedKey
    }

    override fun deriveFileKey(masterKey: ByteArray, fileSalt: ByteArray): ByteArray {
        // HKDF-SHA256 (RFC 5869)
        // Step 1: HKDF-Extract(salt=fileSalt, IKM=masterKey) -> PRK
        val prk = hmacSha256(key = fileSalt, data = masterKey)
        // Step 2: HKDF-Expand(PRK=prk, info="vaulta-file-key", L=32) -> OKM
        val info = CryptoConstants.HKDF_INFO_PREFIX.toByteArray(Charsets.UTF_8)
        val expandInput = ByteArray(info.size + 1)
        System.arraycopy(info, 0, expandInput, 0, info.size)
        expandInput[expandInput.size - 1] = 0x01.toByte()

        return try {
            hmacSha256(key = prk, data = expandInput)
        } finally {
            SecureMemory.wipe(prk)
            SecureMemory.wipe(expandInput)
        }
    }

    override fun generateSalt(): ByteArray {
        return randomProvider.nextBytes(CryptoConstants.SALT_SIZE_BYTES)
    }

    override fun generateMasterKey(): ByteArray {
        return randomProvider.nextBytes(CryptoConstants.MASTER_KEY_SIZE_BYTES)
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
