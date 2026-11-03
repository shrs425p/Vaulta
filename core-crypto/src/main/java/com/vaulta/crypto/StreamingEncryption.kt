package com.vaulta.crypto

import okio.BufferedSink
import okio.BufferedSource
import okio.Source

data class StreamHeader(
    val version: Byte,
    val noncePrefix: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StreamHeader
        if (version != other.version) return false
        if (!noncePrefix.contentEquals(other.noncePrefix)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = version.toInt()
        result = 31 * result + noncePrefix.contentHashCode()
        return result
    }
}

interface StreamingEncryption {

    /**
     * Reads and parses the stream header (version + 8-byte nonce prefix) from the ciphertext stream.
     */
    fun readStreamHeader(input: BufferedSource): StreamHeader

    /**
     * Encrypts the source stream in 64KB chunks into the sink.
     *
     * @param input Plaintext input stream.
     * @param output Ciphertext destination.
     * @param fileKey 256-bit derived per-file key.
     * @param totalBytes Total expected size in bytes, or -1 if unknown.
     * @param onProgress Optional callback invoked with (bytesProcessed, totalBytes) after each chunk.
     */
    fun encryptStream(
        input: Source,
        output: BufferedSink,
        fileKey: ByteArray,
        totalBytes: Long = -1L,
        onProgress: ((bytesProcessed: Long, totalBytes: Long) -> Unit)? = null
    )

    /**
     * Decrypts the stream chunk by chunk, verifying integrity and final chunk boundary.
     *
     * @param input Ciphertext stream.
     * @param output Plaintext destination.
     * @param fileKey 256-bit derived per-file key.
     */
    fun decryptStream(input: BufferedSource, output: BufferedSink, fileKey: ByteArray)

    /**
     * Decrypts an individual chunk at the specified chunk index.
     * Used for random access (e.g., video seeking, range requests).
     */
    fun decryptChunk(
        input: BufferedSource,
        fileKey: ByteArray,
        chunkIndex: Long,
        streamNoncePrefix: ByteArray,
        version: Byte = 2
    ): ByteArray
}
