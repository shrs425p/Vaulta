package com.vaulta.crypto

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * Pure Kotlin TOTP (Time-Based One-Time Password, RFC 6238) generator.
 *
 * Decodes Base32 secret keys and calculates 6-digit 2FA codes for the current 30-second time window.
 */
object TotpGenerator {

    private const val DEFAULT_TIME_STEP_SECONDS = 30L
    private const val DEFAULT_DIGITS = 6

    /**
     * Calculates the current 6-digit TOTP code for [base32Secret].
     *
     * @param base32Secret The Base32-encoded TOTP secret key (e.g. "JBSWY3DPEHPK3PXP").
     * @param timeMs Current timestamp in milliseconds (defaults to system time).
     * @param periodSeconds Time step duration in seconds (defaults to 30s).
     * @return Formatted 6-digit code string (e.g. "123456"), or empty string if secret is invalid.
     */
    fun generateTotp(
        base32Secret: String,
        timeMs: Long = System.currentTimeMillis(),
        periodSeconds: Long = DEFAULT_TIME_STEP_SECONDS
    ): String {
        val cleanSecret = base32Secret.replace(" ", "").replace("-", "").uppercase()
        val keyBytes = decodeBase32(cleanSecret) ?: return ""

        val counter = (timeMs / 1000L) / periodSeconds
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        return try {
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
            val hash = mac.doFinal(counterBytes)

            val offset = (hash[hash.size - 1].toInt() and 0x0F)
            val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val otp = binary % 10.toDouble().pow(DEFAULT_DIGITS.toDouble()).toInt()
            otp.toString().padStart(DEFAULT_DIGITS, '0')
        } catch (_: Exception) {
            ""
        } finally {
            SecureMemory.wipe(keyBytes)
        }
    }

    /**
     * Calculates the remaining seconds in the current TOTP time window (0-30s).
     */
    fun getRemainingSeconds(
        timeMs: Long = System.currentTimeMillis(),
        periodSeconds: Long = DEFAULT_TIME_STEP_SECONDS
    ): Int {
        val elapsed = (timeMs / 1000L) % periodSeconds
        return (periodSeconds - elapsed).toInt()
    }

    private fun decodeBase32(base32: String): ByteArray? {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = base32.trimEnd('=')
        if (clean.isEmpty()) return null

        val bytes = ByteArray(clean.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var count = 0

        for (c in clean) {
            val charIndex = base32Chars.indexOf(c)
            if (charIndex < 0) return null

            buffer = (buffer shl 5) or charIndex
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bytes[count++] = ((buffer shr (bitsLeft - 8)) and 0xFF).toByte()
                bitsLeft -= 8
            }
        }
        return bytes.copyOf(count)
    }
}
