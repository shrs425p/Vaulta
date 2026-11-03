package com.vaulta.storage

import android.content.Context
import androidx.room.Room
import com.vaulta.storage.blob.BlobStore
import com.vaulta.storage.blob.BlobStoreImpl
import com.vaulta.storage.credentials.CredentialDao
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.storage.index.EncryptedIndexImpl
import com.vaulta.storage.index.VaultDatabase
import com.vaulta.storage.index.VaultEntryDao
import com.vaulta.storage.saf.SafAccessor
import com.vaulta.storage.saf.SafAccessorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageBindingsModule {

    @Binds
    @Singleton
    abstract fun bindEncryptedIndex(impl: EncryptedIndexImpl): EncryptedIndex

    @Binds
    @Singleton
    abstract fun bindBlobStore(impl: BlobStoreImpl): BlobStore

    @Binds
    @Singleton
    abstract fun bindSafAccessor(impl: SafAccessorImpl): SafAccessor
}

@Module
@InstallIn(SingletonComponent::class)
object StorageDatabaseModule {

    @Provides
    @Singleton
    fun provideVaultDatabase(
        @ApplicationContext context: Context
    ): VaultDatabase {
        // In-memory Room database: zero plaintext metadata ever written to disk/flash storage.
        // Persistence is securely managed through EncryptedIndex (AES-256-GCM vault_index.enc).
        return Room.inMemoryDatabaseBuilder(
            context,
            VaultDatabase::class.java
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideVaultEntryDao(database: VaultDatabase): VaultEntryDao {
        return database.vaultEntryDao()
    }

    @Provides
    fun provideCredentialDao(database: VaultDatabase): CredentialDao {
        return database.credentialDao()
    }
}
