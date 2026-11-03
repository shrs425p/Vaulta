package com.vaulta.crypto

import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.pow

enum class PasswordStrength(val label: String, val score: Float) {
    WEAK("Weak", 0.25f),
    FAIR("Fair", 0.50f),
    STRONG("Strong", 0.75f),
    VERY_STRONG("Very Strong", 1.0f)
}

object PasswordGenerator {

    private val random = SecureRandom()

    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    private val WORD_LIST = listOf(
        "vault", "secure", "argon", "cipher", "anchor", "beacon", "castle", "dragon",
        "ember", "forest", "galaxy", "harbor", "island", "jungle", "kingdom", "lantern",
        "matrix", "nebula", "ocean", "planet", "quartz", "river", "shadow", "timber",
        "umbrella", "valley", "wisdom", "xenon", "yellow", "zenith", "alpine", "breeze",
        "canyon", "desert", "echo", "falcon", "glacier", "horizon", "impact", "journey",
        "knight", "legend", "mirage", "nature", "orbital", "pioneer", "quantum", "radiant",
        "summit", "thunder", "utopia", "vortex", "wild", "yacht", "zodiac", "prism"
    )

    fun generatePassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val charset = StringBuilder().apply {
            if (includeLower) append(LOWERCASE)
            if (includeUpper) append(UPPERCASE)
            if (includeDigits) append(DIGITS)
            if (includeSymbols) append(SYMBOLS)
        }.toString()

        if (charset.isEmpty()) return ""

        val result = CharArray(length.coerceIn(8, 128))
        for (i in result.indices) {
            result[i] = charset[random.nextInt(charset.length)]
        }
        return String(result)
    }

    fun generatePassphrase(wordCount: Int = 4, separator: String = "-"): String {
        val count = wordCount.coerceIn(3, 10)
        val selectedWords = List(count) { WORD_LIST[random.nextInt(WORD_LIST.size)] }
        return selectedWords.joinToString(separator)
    }

    fun calculateStrength(password: String): PasswordStrength {
        if (password.isBlank()) return PasswordStrength.WEAK

        var poolSize = 0
        if (password.any { it.isLowerCase() }) poolSize += LOWERCASE.length
        if (password.any { it.isUpperCase() }) poolSize += UPPERCASE.length
        if (password.any { it.isDigit() }) poolSize += DIGITS.length
        if (password.any { !it.isLetterOrDigit() }) poolSize += SYMBOLS.length

        if (poolSize == 0) poolSize = 26

        val entropy = password.length * log2(poolSize.toDouble())

        return when {
            entropy < 36 -> PasswordStrength.WEAK
            entropy < 60 -> PasswordStrength.FAIR
            entropy < 80 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
}
