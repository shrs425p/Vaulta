package com.vaulta.storage.credentials

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {

    @Query("SELECT * FROM vault_credentials ORDER BY isFavorite DESC, title ASC")
    fun getAllCredentials(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM vault_credentials WHERE category = :category ORDER BY isFavorite DESC, title ASC")
    fun getCredentialsByCategory(category: String): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM vault_credentials WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchCredentials(query: String): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM vault_credentials WHERE id = :id")
    suspend fun getCredentialById(id: String): CredentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: CredentialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredentials(credentials: List<CredentialEntity>)

    @Update
    suspend fun updateCredential(credential: CredentialEntity)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)

    @Query("SELECT * FROM vault_credentials")
    suspend fun getAllCredentialsList(): List<CredentialEntity>

    @Query("DELETE FROM vault_credentials")
    suspend fun deleteAll()
}
