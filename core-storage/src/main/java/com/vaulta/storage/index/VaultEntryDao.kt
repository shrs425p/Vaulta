package com.vaulta.storage.index

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultEntryDao {

    @Query("SELECT * FROM vault_entries WHERE (parentId = :parentId OR (:parentId IS NULL AND parentId IS NULL)) AND syncStatus != 'DELETED' ORDER BY isDirectory DESC, name ASC")
    fun getEntriesByParent(parentId: String?): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE isDirectory = 1 AND syncStatus != 'DELETED' ORDER BY name ASC")
    fun getAllFolders(): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getEntryById(id: String): VaultEntryEntity?

    @Query("SELECT * FROM vault_entries WHERE name LIKE '%' || :query || '%' AND syncStatus != 'DELETED' ORDER BY isDirectory DESC, name ASC")
    fun searchEntries(query: String): Flow<List<VaultEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: VaultEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<VaultEntryEntity>)

    @Update
    suspend fun updateEntry(entry: VaultEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: VaultEntryEntity)

    @Query("SELECT * FROM vault_entries WHERE syncStatus = :status")
    suspend fun getEntriesBySyncStatus(status: String): List<VaultEntryEntity>

    @Query("SELECT * FROM vault_entries")
    suspend fun getAllEntries(): List<VaultEntryEntity>

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM vault_entries WHERE syncStatus != 'DELETED'")
    suspend fun getEntryCount(): Int
}
