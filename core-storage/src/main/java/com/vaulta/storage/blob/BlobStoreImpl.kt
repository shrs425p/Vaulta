package com.vaulta.storage.blob

import android.content.Context
import com.vaulta.crypto.StreamingEncryption
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.Buffer
import okio.BufferedSource
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlobStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamingEncryption: StreamingEncryption
) : BlobStore {

    private val blobsDir: File by lazy {
        File(context.filesDir, "vaulta/blobs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override fun writeBlob(
        blobName: String,
        input: Source,
        fileKey: ByteArray,
        totalBytes: Long,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)?
    ) {
        val targetFile = File(blobsDir, blobName)
        targetFile.sink().buffer().use { sink ->
            streamingEncryption.encryptStream(input, sink, fileKey, totalBytes, onProgress)
        }
    }

    override fun readBlob(blobName: String, fileKey: ByteArray): Source {
        val targetFile = File(blobsDir, blobName)
        val fileSource = targetFile.source().buffer()
        return DecryptingBlobSource(fileSource, streamingEncryption, fileKey)
    }

    override fun readBlobBytes(blobName: String, fileKey: ByteArray): ByteArray {
        val targetFile = File(blobsDir, blobName)
        val decryptedBuffer = Buffer()
        targetFile.source().buffer().use { source ->
            streamingEncryption.decryptStream(source, decryptedBuffer, fileKey)
        }
        return decryptedBuffer.readByteArray()
    }

    override fun decryptBlobToSink(blobName: String, sink: Sink, fileKey: ByteArray) {
        val targetFile = File(blobsDir, blobName)
        targetFile.source().buffer().use { source ->
            sink.buffer().use { bufferedSink ->
                streamingEncryption.decryptStream(source, bufferedSink, fileKey)
            }
        }
    }

    override fun deleteBlob(blobName: String): Boolean {
        val targetFile = File(blobsDir, blobName)
        return if (targetFile.exists()) targetFile.delete() else true
    }

    override fun getBlobFile(blobName: String): File {
        return File(blobsDir, blobName)
    }

    override fun getBlobSize(blobName: String): Long {
        val targetFile = File(blobsDir, blobName)
        return if (targetFile.exists()) targetFile.length() else 0L
    }

    override fun listBlobs(): List<String> {
        return blobsDir.list()?.toList() ?: emptyList()
    }

    override fun getBlobsDirectory(): File {
        return blobsDir
    }

    private class DecryptingBlobSource(
        private val input: BufferedSource,
        private val streamingEncryption: StreamingEncryption,
        private val fileKey: ByteArray
    ) : Source {
        private val chunkBuffer = Buffer()
        private var streamHeader: com.vaulta.crypto.StreamHeader? = null
        private var chunkIndex = 0L
        private var isExhausted = false
        private var closed = false

        init {
            if (!input.exhausted()) {
                streamHeader = streamingEncryption.readStreamHeader(input)
            } else {
                isExhausted = true
            }
        }

        override fun read(sink: Buffer, byteCount: Long): Long {
            if (closed) throw IllegalStateException("closed")
            if (byteCount == 0L) return 0L

            while (chunkBuffer.exhausted() && !isExhausted) {
                decryptNextChunk()
            }

            if (chunkBuffer.exhausted()) {
                return -1L
            }

            return chunkBuffer.read(sink, byteCount)
        }

        private fun decryptNextChunk() {
            if (input.exhausted()) {
                isExhausted = true
                return
            }

            val header = streamHeader ?: throw IOException("Missing stream header")
            val plaintext = streamingEncryption.decryptChunk(
                input = input,
                fileKey = fileKey,
                chunkIndex = chunkIndex,
                streamNoncePrefix = header.noncePrefix,
                version = header.version
            )
            chunkBuffer.write(plaintext)
            chunkIndex++
        }

        override fun timeout(): Timeout = input.timeout()

        override fun close() {
            if (!closed) {
                closed = true
                chunkBuffer.clear()
                input.close()
            }
        }
    }
}
