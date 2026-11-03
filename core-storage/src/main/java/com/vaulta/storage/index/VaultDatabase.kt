package com.vaulta.storage.index

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vaulta.storage.credentials.CredentialDao
import com.vaulta.storage.credentials.CredentialEntity

@Database(
    entities = [VaultEntryEntity::class, CredentialEntity::class],
    version = 2,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultEntryDao(): VaultEntryDao
    abstract fun credentialDao(): CredentialDao
}
