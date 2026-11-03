package com.vaulta.crypto

/**
 * Thrown when key unwrapping fails, typically due to an incorrect wrapping key
 * or corrupted wrapped key data.
 */
class KeyUnwrapException(
    message: String = "Failed to unwrap key: incorrect wrapping key or corrupted data",
    cause: Throwable? = null,
) : Exception(message, cause)
