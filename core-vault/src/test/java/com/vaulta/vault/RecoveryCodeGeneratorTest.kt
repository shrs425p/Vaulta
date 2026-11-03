package com.vaulta.vault

import com.google.common.truth.Truth.assertThat
import com.vaulta.vault.recovery.RecoveryCodeGenerator
import com.vaulta.vault.recovery.RecoveryCodeGeneratorImpl
import org.junit.Before
import org.junit.Test

class RecoveryCodeGeneratorTest {

    private lateinit var generator: RecoveryCodeGenerator

    @Before
    fun setUp() {
        generator = RecoveryCodeGeneratorImpl()
    }

    @Test
    fun generate_returnsFormattedCode() {
        val code = generator.generate()
        // 8 groups of 4 separated by 7 dashes -> 32 chars + 7 dashes = 39 characters
        assertThat(code.length).isEqualTo(39)
        val groups = code.split("-")
        assertThat(groups.size).isEqualTo(8)
        for (group in groups) {
            assertThat(group.length).isEqualTo(4)
        }
    }

    @Test
    fun toCharArray_stripsDashes() {
        val code = generator.generate()
        val chars = generator.toCharArray(code)
        assertThat(chars.size).isEqualTo(32)
        assertThat(chars.none { it == '-' }).isTrue()
    }

    @Test
    fun generate_twoCodesAreDifferent() {
        val code1 = generator.generate()
        val code2 = generator.generate()
        assertThat(code1).isNotEqualTo(code2)
    }
}
