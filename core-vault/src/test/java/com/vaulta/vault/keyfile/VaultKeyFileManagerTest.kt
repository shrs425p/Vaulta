package com.vaulta.vault.keyfile

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class VaultKeyFileManagerTest {
    @Test
    fun readBoundedBytes_acceptsDataWithinLimit() {
        val data = ByteArray(128) { it.toByte() }
        assertThat(ByteArrayInputStream(data).readBoundedBytes(128)).isEqualTo(data)
    }

    @Test
    fun readBoundedBytes_rejectsOversizedInput() {
        assertThrows(InvalidVaultKeyFileException::class.java) {
            ByteArrayInputStream(ByteArray(1024 * 1024)).readBoundedBytes(64)
        }
    }
}
