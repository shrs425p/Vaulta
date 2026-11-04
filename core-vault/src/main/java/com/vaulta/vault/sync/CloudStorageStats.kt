package com.vaulta.vault.sync

data class CloudBlobInfo(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val modifiedTimeMs: Long
)

data class CloudStorageStats(
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val isStealthSpace: Boolean,
    val blobs: List<CloudBlobInfo>
)
