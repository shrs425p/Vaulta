package com.vaulta.storage.index

import kotlinx.coroutines.flow.Flow

interface EncryptedIndex {

    /**
     * Loads the index from the authenticated encrypted storage file (`vault_index.enc`).
     * If `vault_index.enc` does not exist, checks for a legacy unencrypted SQLite database
     * (`vaulta_local.db`), migrates its entries, saves the encrypted index, and permanently
     * shreds the plaintext SQLite files.
     */
    suspend fun loadIndex(masterKey: ByteArray)

    /**
     * Serializes all metadata entries from the in-memory database and writes them encrypted
     * with AES-256-GCM to `vault_index.enc`.
     */
    suspend fun saveIndex(masterKey: ByteArray)

    suspend fun loadFromEncryptedData(encryptedBytes: ByteArray, masterKey: ByteArray)

    suspend fun parseFromEncryptedData(encryptedBytes: ByteArray, masterKey: ByteArray): List<VaultEntry>

    suspend fun exportToEncryptedData(masterKey: ByteArray): ByteArray

    suspend fun addEntry(entry: VaultEntry)

    suspend fun updateEntry(entry: VaultEntry)

    suspend fun removeEntry(id: String)

    fun getEntriesByParent(parentId: String?): Flow<List<VaultEntry>>

    fun getAllFolders(): Flow<List<VaultEntry>>

    suspend fun getEntry(id: String): VaultEntry?

    fun searchEntries(query: String): Flow<List<VaultEntry>>

    suspend fun clear()

    suspend fun getEntryCount(): Int

    suspend fun getVaultStats(): VaultStats
}

data class VaultStats(
    val fileCount: Int,
    val folderCount: Int,
    val totalSizeBytes: Long
)
