package com.vaulta.storage.index

enum class SyncStatus {
    SYNCED,
    PENDING,
    DELETED,
    CONFLICT
}

data class VaultEntry(
    val id: String,
    val name: String,
    val parentId: String?,
    val isDirectory: Boolean,
    val mimeType: String?,
    val sizeBytes: Long,
    val blobName: String?,
    val fileSalt: ByteArray?,
    val createdAt: Long,
    val modifiedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VaultEntry) return false

        if (id != other.id) return false
        if (name != other.name) return false
        if (parentId != other.parentId) return false
        if (isDirectory != other.isDirectory) return false
        if (mimeType != other.mimeType) return false
        if (sizeBytes != other.sizeBytes) return false
        if (blobName != other.blobName) return false
        if (fileSalt != null) {
            if (other.fileSalt == null) return false
            if (!fileSalt.contentEquals(other.fileSalt)) return false
        } else if (other.fileSalt != null) return false
        if (createdAt != other.createdAt) return false
        if (modifiedAt != other.modifiedAt) return false
        if (syncStatus != other.syncStatus) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (parentId?.hashCode() ?: 0)
        result = 31 * result + isDirectory.hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + (blobName?.hashCode() ?: 0)
        result = 31 * result + (fileSalt?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + modifiedAt.hashCode()
        result = 31 * result + syncStatus.hashCode()
        return result
    }
}
