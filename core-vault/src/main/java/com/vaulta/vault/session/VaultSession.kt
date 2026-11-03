package com.vaulta.vault.session

import com.vaulta.crypto.SecureMemory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the master key in memory while the vault is unlocked.
 *
 * This is a critical security class. The master key is held only as a [ByteArray]
 * (never as a [String]) and is zeroed out when the vault is locked. All access
 * is synchronized to prevent data races.
 *
 * Key security properties:
 * - [getMasterKey] returns a **copy** of the key, so callers cannot mutate the internal state.
 * - [lock] zeroes out the internal key array before dropping the reference.
 * - [unlock] stores a **copy** of the provided key, so callers can wipe their copy independently.
 */
@Singleton
class VaultSession @Inject constructor() {

    private var _masterKey: ByteArray? = null

    private val _isUnlocked = MutableStateFlow(false)

    /** Observable state indicating whether the vault is currently unlocked. */
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    /**
     * Unlocks the vault by storing a copy of the provided master key.
     *
     * If the vault is already unlocked, the previous key is wiped before
     * storing the new one.
     *
     * @param masterKey The 256-bit master key. A copy is made; the caller
     *                  should wipe this array after calling.
     */
    @Synchronized
    fun unlock(masterKey: ByteArray) {
        // Wipe any existing key before replacing
        _masterKey?.fill(0)
        _masterKey = masterKey.copyOf()
        _isUnlocked.value = true
    }

    /**
     * Returns a **copy** of the master key.
     *
     * The caller is responsible for wiping the returned array when done.
     *
     * @return A copy of the master key.
     * @throws VaultLockedException if the vault is locked.
     */
    @Synchronized
    fun getMasterKey(): ByteArray {
        val key = _masterKey
            ?: throw VaultLockedException()
        return key.copyOf()
    }

    /**
     * Executes the given [block] with a copy of the master key, guaranteeing that the key
     * is securely wiped immediately after the block finishes, even if an exception is thrown.
     */
    inline fun <R> withMasterKey(block: (ByteArray) -> R): R {
        val key = getMasterKey()
        return try {
            block(key)
        } finally {
            SecureMemory.wipe(key)
        }
    }

    /**
     * Locks the vault by wiping the master key from memory.
     *
     * After this call, [getMasterKey] will throw [VaultLockedException]
     * and [isUnlocked] will emit `false`.
     */
    @Synchronized
    fun lock() {
        _masterKey?.fill(0)
        _masterKey = null
        _isUnlocked.value = false
    }
}
