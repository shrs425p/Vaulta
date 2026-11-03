package com.vaulta.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun generatePassword_respectsLengthAndCharset() {
        val password = PasswordGenerator.generatePassword(length = 20)
        assertThat(password.length).isEqualTo(20)
    }

    @Test
    fun generatePassphrase_joinsWordsWithSeparator() {
        val passphrase = PasswordGenerator.generatePassphrase(wordCount = 4, separator = "-")
        val words = passphrase.split("-")
        assertThat(words.size).isEqualTo(4)
    }

    @Test
    fun calculateStrength_scoresEntropyCorrectly() {
        val weak = PasswordGenerator.calculateStrength("12345")
        val strong = PasswordGenerator.calculateStrength("P@ssw0rd!LongAndComplex2025")

        assertThat(weak).isEqualTo(PasswordStrength.WEAK)
        assertThat(strong).isEqualTo(PasswordStrength.VERY_STRONG)
    }
}
