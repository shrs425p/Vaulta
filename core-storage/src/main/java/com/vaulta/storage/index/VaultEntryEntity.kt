package com.vaulta.storage.index

import java.util.Base64
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_entries")
data class VaultEntryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String?,
    val isDirectory: Boolean,
    val mimeType: String?,
    val sizeBytes: Long,
    val blobName: String?,
    val fileSaltBase64: String?,
    val createdAt: Long,
    val modifiedAt: Long,
    val syncStatus: String
)

fun VaultEntry.toEntity(): VaultEntryEntity {
    return VaultEntryEntity(
        id = id,
        name = name,
        parentId = parentId,
        isDirectory = isDirectory,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        blobName = blobName,
        fileSaltBase64 = fileSalt?.let { Base64.getEncoder().encodeToString(it) },
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        syncStatus = syncStatus.name
    )
}

fun VaultEntryEntity.toDomain(): VaultEntry {
    return VaultEntry(
        id = id,
        name = name,
        parentId = parentId,
        isDirectory = isDirectory,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        blobName = blobName,
        fileSalt = fileSaltBase64?.let {
            try {
                Base64.getDecoder().decode(it)
            } catch (_: Exception) {
                null
            }
        },
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        syncStatus = try {
            SyncStatus.valueOf(syncStatus)
        } catch (e: Exception) {
            SyncStatus.PENDING
        }
    )
}
