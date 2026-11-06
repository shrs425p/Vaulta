package com.vaulta.vault

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VaultPasswordPolicyTest {
    @Test
    fun acceptsOnlyConfiguredNewPasswordLengthRange() {
        assertThat(VaultPasswordPolicy.accepts(CharArray(VaultPasswordPolicy.MIN_LENGTH))).isTrue()
        assertThat(VaultPasswordPolicy.accepts(CharArray(VaultPasswordPolicy.MIN_LENGTH - 1))).isFalse()
        assertThat(VaultPasswordPolicy.accepts(CharArray(VaultPasswordPolicy.MAX_LENGTH))).isTrue()
        assertThat(VaultPasswordPolicy.accepts(CharArray(VaultPasswordPolicy.MAX_LENGTH + 1))).isFalse()
    }
}
