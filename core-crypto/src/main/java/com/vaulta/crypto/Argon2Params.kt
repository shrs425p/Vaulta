package com.vaulta.crypto

/**
 * Parameters for the Argon2id key derivation function.
 *
 * @property memoryCostKb Memory cost in kilobytes.
 * @property iterations Number of iterations (time cost).
 * @property parallelism Degree of parallelism.
 */
data class Argon2Params(
    val memoryCostKb: Int = 65536,   // 64 MB
    val iterations: Int = 3,
    val parallelism: Int = 4,
)
