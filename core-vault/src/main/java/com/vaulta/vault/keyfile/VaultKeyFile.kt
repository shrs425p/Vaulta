package com.vaulta.vault.keyfile

import com.vaulta.crypto.Argon2Params

/**
 * Data class representing the vault.key file structure.
 *
 * The vault.key file stores the encrypted master key in two slots:
 * - Slot A: wrapped with a key derived from the user's password.
 * - Slot B: wrapped with a key derived from the recovery code.
 *
 * @property magic File magic identifier, always "VAULTA" (6 bytes).
 * @property version File format version (1 byte).
 * @property argon2Params Argon2id parameters used for key derivation.
 * @property salt Random 32-byte salt for password-based key derivation.
 * @property wrappedMasterKeySlotA Master key wrapped with password-derived key.
 * @property wrappedMasterKeySlotB Master key wrapped with recovery-code-derived key.
 */
data class VaultKeyFile(
    val magic: String = MAGIC,
    val version: Int = CURRENT_VERSION,
    val argon2Params: Argon2Params,
    val salt: ByteArray,
    val wrappedMasterKeySlotA: ByteArray,
    val wrappedMasterKeySlotB: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VaultKeyFile) return false

        if (magic != other.magic) return false
        if (version != other.version) return false
        if (argon2Params != other.argon2Params) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!wrappedMasterKeySlotA.contentEquals(other.wrappedMasterKeySlotA)) return false
        if (!wrappedMasterKeySlotB.contentEquals(other.wrappedMasterKeySlotB)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = magic.hashCode()
        result = 31 * result + version
        result = 31 * result + argon2Params.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + wrappedMasterKeySlotA.contentHashCode()
        result = 31 * result + wrappedMasterKeySlotB.contentHashCode()
        return result
    }

    companion object {
        /** File magic identifier. */
        const val MAGIC = "VAULTA"

        /** Current file format version. */
        const val CURRENT_VERSION = 1
    }
}
