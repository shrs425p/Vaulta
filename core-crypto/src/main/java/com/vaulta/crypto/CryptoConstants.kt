package com.vaulta.crypto

object CryptoConstants {
    const val MASTER_KEY_SIZE_BYTES: Int = 32
    const val SALT_SIZE_BYTES: Int = 32
    const val CHUNK_SIZE_BYTES: Int = 64 * 1024 // 64 KB
    const val NONCE_SIZE_BYTES: Int = 12
    const val TAG_SIZE_BYTES: Int = 16
    const val HKDF_INFO_PREFIX: String = "vaulta-file-key"
    const val VAULT_KEY_MAGIC: String = "VAULTA"
    const val VAULT_KEY_VERSION: Int = 1
}
