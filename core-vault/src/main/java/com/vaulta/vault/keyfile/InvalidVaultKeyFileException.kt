package com.vaulta.vault.keyfile

/**
 * Thrown when a vault.key file cannot be deserialized because it has
 * an invalid magic header, unsupported version, or corrupted data.
 */
class InvalidVaultKeyFileException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
