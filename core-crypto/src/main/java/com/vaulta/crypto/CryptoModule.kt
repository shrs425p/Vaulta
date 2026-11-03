package com.vaulta.crypto

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    @Binds
    @Singleton
    abstract fun bindSecureRandomProvider(
        impl: SecureRandomProviderImpl
    ): SecureRandomProvider

    @Binds
    @Singleton
    abstract fun bindKeyDerivation(
        impl: KeyDerivationImpl
    ): KeyDerivation

    @Binds
    @Singleton
    abstract fun bindKeyWrapping(
        impl: KeyWrappingImpl
    ): KeyWrapping

    @Binds
    @Singleton
    abstract fun bindStreamingEncryption(
        impl: StreamingEncryptionImpl
    ): StreamingEncryption
}
