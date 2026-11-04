package com.vaulta.crypto

import java.security.SecureRandom
import kotlin.math.log2

enum class PasswordStrength(val label: String, val score: Float) {
    VERY_WEAK("Very Weak", 0.15f),
    WEAK("Weak", 0.30f),
    FAIR("Fair", 0.55f),
    STRONG("Strong", 0.80f),
    VERY_STRONG("Very Strong", 1.0f)
}

data class PasswordAnalysis(
    val entropyBits: Double,
    val strength: PasswordStrength,
    val crackTimeDisplay: String
)

object PasswordGenerator {

    private val random = SecureRandom()

    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    private const val AMBIGUOUS = "l1IO0"

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
        includeSymbols: Boolean = true,
        avoidAmbiguous: Boolean = false
    ): String {
        var lower = LOWERCASE
        var upper = UPPERCASE
        var digits = DIGITS
        var symbols = SYMBOLS

        if (avoidAmbiguous) {
            lower = lower.filterNot { it in AMBIGUOUS }
            upper = upper.filterNot { it in AMBIGUOUS }
            digits = digits.filterNot { it in AMBIGUOUS }
            symbols = symbols.filterNot { it in AMBIGUOUS }
        }

        val charset = StringBuilder().apply {
            if (includeLower) append(lower)
            if (includeUpper) append(upper)
            if (includeDigits) append(digits)
            if (includeSymbols) append(symbols)
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

    fun calculateEntropyBits(password: String): Double {
        if (password.isEmpty()) return 0.0

        var poolSize = 0
        if (password.any { it.isLowerCase() }) poolSize += LOWERCASE.length
        if (password.any { it.isUpperCase() }) poolSize += UPPERCASE.length
        if (password.any { it.isDigit() }) poolSize += DIGITS.length
        if (password.any { !it.isLetterOrDigit() }) poolSize += SYMBOLS.length

        if (poolSize == 0) poolSize = 26

        return password.length * log2(poolSize.toDouble())
    }

    fun analyzePassword(password: String): PasswordAnalysis {
        val bits = calculateEntropyBits(password)
        val strength = when {
            bits < 28 -> PasswordStrength.VERY_WEAK
            bits < 40 -> PasswordStrength.WEAK
            bits < 60 -> PasswordStrength.FAIR
            bits < 80 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }

        val crackTime = calculateCrackTimeDisplay(bits)
        return PasswordAnalysis(
            entropyBits = (bits * 10.0).let { Math.round(it) / 10.0 },
            strength = strength,
            crackTimeDisplay = crackTime
        )
    }

    private fun calculateCrackTimeDisplay(entropyBits: Double): String {
        if (entropyBits <= 0) return "Instant"
        val combinations = Math.pow(2.0, entropyBits)
        val seconds = combinations / 10_000_000_000.0

        return when {
            seconds < 1 -> "Instant"
            seconds < 60 -> "${seconds.toInt()} seconds"
            seconds < 3600 -> "${(seconds / 60).toInt()} minutes"
            seconds < 86400 -> "${(seconds / 3600).toInt()} hours"
            seconds < 2592000 -> "${(seconds / 86400).toInt()} days"
            seconds < 31536000 -> "${(seconds / 2592000).toInt()} months"
            seconds < 3153600000.0 -> "${(seconds / 31536000).toInt()} years"
            else -> "Centuries"
        }
    }

    fun calculateStrength(password: String): PasswordStrength {
        return analyzePassword(password).strength
    }
}
