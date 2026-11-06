package com.vaulta.vault

/** Length bounds for passwords newly used to wrap a vault master key. */
object VaultPasswordPolicy {
    const val MIN_LENGTH = 15
    const val MAX_LENGTH = 1024

    fun accepts(password: CharArray): Boolean = password.size in MIN_LENGTH..MAX_LENGTH
}
