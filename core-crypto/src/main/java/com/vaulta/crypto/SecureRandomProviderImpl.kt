package com.vaulta.crypto

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureRandomProviderImpl @Inject constructor() : SecureRandomProvider {

    private val secureRandom = SecureRandom()

    override fun nextBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }
}
