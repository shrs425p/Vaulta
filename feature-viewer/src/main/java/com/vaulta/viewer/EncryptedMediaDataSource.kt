package com.vaulta.viewer

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.vaulta.crypto.SecureMemory
import com.vaulta.crypto.StreamingEncryption
import java.io.File
import java.io.IOException
import okio.BufferedSource
import okio.buffer
import okio.source

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class EncryptedMediaDataSource private constructor(
    private val encryptedFile: File,
    private val streamingEncryption: StreamingEncryption,
    private val fileKey: ByteArray,
    private val contentLength: Long
) : DataSource {

    private var input: BufferedSource? = null
    private var currentChunk = ByteArray(0)
    private var currentOffset = 0
    private var chunkIndex = 0L
    private var absolutePosition = 0L
    private var bytesRemaining = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) = Unit

    override fun open(dataSpec: DataSpec): Long {
        if (opened) throw IOException("Data source already opened")
        if (dataSpec.position < 0L || dataSpec.position > contentLength) {
            throw IOException("Invalid media position")
        }

        val source = encryptedFile.source().buffer()
        try {
            val header = streamingEncryption.readStreamHeader(source)
            input = source
            opened = true
            bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                contentLength - dataSpec.position
            } else {
                minOf(dataSpec.length, contentLength - dataSpec.position)
            }
            absolutePosition = dataSpec.position
            skipToPosition(dataSpec.position, header)
            return bytesRemaining
        } catch (error: Exception) {
            source.close()
            SecureMemory.wipe(fileKey)
            throw IOException("Unable to open encrypted media", error)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!opened) throw IOException("Data source is not open")
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        if (currentOffset == currentChunk.size) {
            val source = input ?: throw IOException("Missing encrypted media source")
            if (source.exhausted()) {
                throw IOException("Encrypted media is truncated before its final chunk")
            }
            val isLastChunk = source.peek().apply { skip(4L) }.readByte().toInt() == 1
            currentChunk = streamingEncryption.decryptChunk(
                input = source,
                fileKey = fileKey,
                chunkIndex = chunkIndex++,
                streamNoncePrefix = streamHeader!!.noncePrefix,
                version = streamHeader!!.version
            )
            currentOffset = 0
            if (currentChunk.isEmpty() && !isLastChunk) {
                throw IOException("Encrypted media contains an empty non-final chunk")
            }
            if (isLastChunk && currentChunk.size.toLong() != contentLength - absolutePosition) {
                throw IOException("Encrypted media length does not match its authenticated final chunk")
            }
        }

        val available = currentChunk.size - currentOffset
        val requested = minOf(length.toLong(), available.toLong(), bytesRemaining).toInt()
        currentChunk.copyInto(buffer, offset, currentOffset, currentOffset + requested)
        currentOffset += requested
        bytesRemaining -= requested
        absolutePosition += requested
        if (currentOffset == currentChunk.size) {
            SecureMemory.wipe(currentChunk)
        }
        return requested
    }

    override fun getUri(): Uri? = Uri.fromFile(encryptedFile)

    override fun close() {
        input?.close()
        input = null
        SecureMemory.wipe(currentChunk)
        SecureMemory.wipe(fileKey)
        currentChunk = ByteArray(0)
        opened = false
    }

    private var streamHeader: com.vaulta.crypto.StreamHeader? = null

    private fun skipToPosition(position: Long, header: com.vaulta.crypto.StreamHeader) {
        streamHeader = header
        var skipped = 0L
        while (skipped < position) {
            val source = input ?: throw IOException("Missing encrypted media source")
            if (source.exhausted()) throw IOException("Encrypted media is truncated")
            val chunk = streamingEncryption.decryptChunk(
                input = source,
                fileKey = fileKey,
                chunkIndex = chunkIndex++,
                streamNoncePrefix = header.noncePrefix,
                version = header.version
            )
            val next = skipped + chunk.size
            if (position < next) {
                currentChunk = chunk
                currentOffset = (position - skipped).toInt()
                return
            }
            SecureMemory.wipe(chunk)
            skipped = next
        }
    }

    class Factory(
        private val encryptedFile: File,
        private val streamingEncryption: StreamingEncryption,
        fileKey: ByteArray,
        private val contentLength: Long
    ) : DataSource.Factory {
        private val keyTemplate = fileKey.copyOf()

        override fun createDataSource(): DataSource {
            return EncryptedMediaDataSource(
                encryptedFile = encryptedFile,
                streamingEncryption = streamingEncryption,
                fileKey = keyTemplate.copyOf(),
                contentLength = contentLength
            )
        }

        fun close() {
            SecureMemory.wipe(keyTemplate)
        }
    }
}
