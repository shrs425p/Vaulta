package com.vaulta.vault

import com.vaulta.vault.keyfile.VaultKeyFileManager
import com.vaulta.vault.keyfile.VaultKeyFileManagerImpl
import com.vaulta.vault.keyfile.VaultKeyFileSerializer
import com.vaulta.vault.keyfile.VaultKeyFileSerializerImpl
import com.vaulta.vault.recovery.RecoveryCodeGenerator
import com.vaulta.vault.recovery.RecoveryCodeGeneratorImpl
import com.vaulta.vault.session.AutoLockManager
import com.vaulta.vault.session.AutoLockManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VaultModule {

    @Binds
    @Singleton
    abstract fun bindVaultKeyFileSerializer(
        impl: VaultKeyFileSerializerImpl
    ): VaultKeyFileSerializer

    @Binds
    @Singleton
    abstract fun bindVaultKeyFileManager(
        impl: VaultKeyFileManagerImpl
    ): VaultKeyFileManager

    @Binds
    @Singleton
    abstract fun bindRecoveryCodeGenerator(
        impl: RecoveryCodeGeneratorImpl
    ): RecoveryCodeGenerator

    @Binds
    @Singleton
    abstract fun bindAutoLockManager(
        impl: AutoLockManagerImpl
    ): AutoLockManager

    @Binds
    @Singleton
    abstract fun bindVaultOperations(
        impl: VaultOperationsImpl
    ): VaultOperations
}
