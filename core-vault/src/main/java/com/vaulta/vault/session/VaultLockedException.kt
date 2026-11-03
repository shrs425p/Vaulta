package com.vaulta.vault.session

/**
 * Thrown when an operation requires the vault to be unlocked,
 * but the vault is currently locked.
 */
class VaultLockedException(
    message: String = "Vault is locked. Unlock with password or biometrics first.",
) : Exception(message)
