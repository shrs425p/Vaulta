package com.vaulta.vault

import com.google.common.truth.Truth.assertThat
import com.vaulta.vault.session.VaultLockedException
import com.vaulta.vault.session.VaultSession
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class VaultSessionTest {

    private lateinit var session: VaultSession

    @Before
    fun setUp() {
        session = VaultSession()
    }

    @Test
    fun getMasterKey_whileLocked_throwsVaultLockedException() {
        assertThat(session.isUnlocked.value).isFalse()
        assertThrows(VaultLockedException::class.java) {
            session.getMasterKey()
        }
    }

    @Test
    fun unlock_and_getMasterKey_returnsCopy() {
        val originalKey = ByteArray(32) { 0x55.toByte() }
        session.unlock(originalKey)

        assertThat(session.isUnlocked.value).isTrue()
        val retrieved = session.getMasterKey()
        assertThat(retrieved).isEqualTo(originalKey)

        // Mutating returned copy should not affect internal state
        retrieved[0] = 0x00
        val secondRetrieve = session.getMasterKey()
        assertThat(secondRetrieve[0]).isEqualTo(0x55.toByte())
    }

    @Test
    fun lock_wipesMasterKey() {
        val originalKey = ByteArray(32) { 0x77.toByte() }
        session.unlock(originalKey)
        assertThat(session.isUnlocked.value).isTrue()

        session.lock()
        assertThat(session.isUnlocked.value).isFalse()

        assertThrows(VaultLockedException::class.java) {
            session.getMasterKey()
        }
    }
}
