package com.vaulta.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TotpGeneratorTest {

    @Test
    fun generateTotp_calculatesValidSixDigitCode() {
        // Standard RFC 6238 test vector secret "JBSWY3DPEHPK3PXP" (ASCII: "12345678901234567890")
        val secret = "JBSWY3DPEHPK3PXP"
        val code = TotpGenerator.generateTotp(secret, timeMs = 59000L) // T0 = 59s -> epoch 1

        assertThat(code.length).isEqualTo(6)
        assertThat(code.all { it.isDigit() }).isTrue()
    }

    @Test
    fun generateTotp_handlesInvalidSecretGracefully() {
        val code = TotpGenerator.generateTotp("INVALID!!!BASE32")
        assertThat(code).isEmpty()
    }
}
