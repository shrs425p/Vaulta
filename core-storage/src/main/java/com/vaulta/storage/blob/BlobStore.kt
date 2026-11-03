package com.vaulta.storage.blob

import okio.Sink
import okio.Source
import java.io.File

interface BlobStore {

    fun writeBlob(
        blobName: String,
        input: Source,
        fileKey: ByteArray,
        totalBytes: Long = -1L,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)? = null
    )

    fun readBlob(blobName: String, fileKey: ByteArray): Source

    fun readBlobBytes(blobName: String, fileKey: ByteArray): ByteArray

    fun decryptBlobToSink(blobName: String, sink: Sink, fileKey: ByteArray)

    fun decryptBlobToFile(blobName: String, targetFile: File, fileKey: ByteArray)

    fun deleteBlob(blobName: String): Boolean

    fun getBlobFile(blobName: String): File

    fun getBlobSize(blobName: String): Long

    fun listBlobs(): List<String>

    fun getBlobsDirectory(): File
}
