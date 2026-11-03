package com.vaulta.vault.recovery

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and processes recovery codes for vault recovery.
 *
 * Recovery codes are formatted as 8 groups of 4 alphanumeric characters
 * separated by dashes (e.g., "ABCD-1234-EFGH-5678-JKML-9012-MNPQ-3456").
 *
 * Confusing characters are excluded to avoid transcription errors:
 * - '0' and 'O' (zero vs. letter O)
 * - '1', 'I', and 'L' (one vs. letter I vs. letter L)
 */
interface RecoveryCodeGenerator {

    /**
     * Generates a cryptographically random recovery code.
     *
     * @return A formatted recovery code string (e.g., "ABCD-1234-EFGH-5678-JKML-9012-MNPQ-3456").
     */
    fun generate(): String

    /**
     * Strips dashes from a recovery code and converts to a [CharArray]
     * suitable for key derivation.
     *
     * @param code The formatted recovery code string.
     * @return A [CharArray] of the raw alphanumeric characters (no dashes).
     */
    fun toCharArray(code: String): CharArray
}

/**
 * Default implementation of [RecoveryCodeGenerator] using [SecureRandom].
 */
@Singleton
class RecoveryCodeGeneratorImpl @Inject constructor() : RecoveryCodeGenerator {

    override fun generate(): String {
        val random = SecureRandom()
        val groups = mutableListOf<String>()

        repeat(GROUP_COUNT) {
            val group = StringBuilder(GROUP_SIZE)
            repeat(GROUP_SIZE) {
                val index = random.nextInt(ALLOWED_CHARS.length)
                group.append(ALLOWED_CHARS[index])
            }
            groups.add(group.toString())
        }

        return groups.joinToString(SEPARATOR)
    }

    override fun toCharArray(code: String): CharArray {
        return code.replace(SEPARATOR, "").toCharArray()
    }

    companion object {
        /** Number of groups in a recovery code. */
        const val GROUP_COUNT = 8

        /** Number of characters per group. */
        const val GROUP_SIZE = 4

        /** Separator between groups. */
        const val SEPARATOR = "-"

        /**
         * Allowed characters for recovery codes.
         * Excludes confusing characters: 0, O, 1, I, L.
         */
        const val ALLOWED_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    }
}
