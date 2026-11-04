package com.vaulta.crypto

import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Source
import okio.buffer
import java.io.IOException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamingEncryptionImpl @Inject constructor(
    private val randomProvider: SecureRandomProvider
) : StreamingEncryption {

    companion object {
        const val STREAM_VERSION: Byte = 2
        const val STREAM_NONCE_PREFIX_SIZE: Int = 8
        const val CHUNK_DATA_SIZE: Int = CryptoConstants.CHUNK_SIZE_BYTES
        const val MAX_CHUNK_LENGTH: Int = CHUNK_DATA_SIZE + 4 + CryptoConstants.TAG_SIZE_BYTES
    }

    override fun encryptStream(
        input: Source,
        output: BufferedSink,
        fileKey: ByteArray,
        totalBytes: Long,
        onProgress: ((bytesProcessed: Long, totalBytes: Long) -> Unit)?
    ) {
        val bufferedInput = (input as? BufferedSource) ?: input.buffer()
        val streamNoncePrefix = randomProvider.nextBytes(STREAM_NONCE_PREFIX_SIZE)

        // Write Header: version (1 byte) + streamNoncePrefix (8 bytes)
        output.writeByte(STREAM_VERSION.toInt())
        output.write(streamNoncePrefix)

        val readBuffer = Buffer()
        var chunkIndex = 0L
        var bytesProcessed = 0L

        do {
            readBuffer.clear()
            // Try to fill up to CHUNK_DATA_SIZE without discarding any bytes
            while (readBuffer.size < CHUNK_DATA_SIZE && !bufferedInput.exhausted()) {
                val toRead = (CHUNK_DATA_SIZE - readBuffer.size).toLong()
                val bytesRead = bufferedInput.read(readBuffer, toRead)
                if (bytesRead == -1L) break
            }

            val isLast = bufferedInput.exhausted()
            val plainBytes = readBuffer.readByteArray()

            // Traffic / Size Fingerprinting Defense:
            // Pad the final chunk with random bytes so that the encrypted chunk aligns
            // with a standard 64 KB block, preventing traffic and file size fingerprinting.
            val payloadToEncrypt = if (isLast) {
                val paddingLen = CHUNK_DATA_SIZE - plainBytes.size
                val randomPadding = if (paddingLen > 0) randomProvider.nextBytes(paddingLen) else ByteArray(0)
                ByteBuffer.allocate(4 + plainBytes.size + paddingLen).apply {
                    putInt(plainBytes.size)
                    put(plainBytes)
                    put(randomPadding)
                }.array()
            } else {
                plainBytes
            }

            val nonce = buildNonce(streamNoncePrefix, chunkIndex)
            val aad = buildAad(chunkIndex, isLast)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(fileKey, "AES"),
                GCMParameterSpec(CryptoConstants.TAG_SIZE_BYTES * 8, nonce)
            )
            cipher.updateAAD(aad)
            val ciphertextAndTag = cipher.doFinal(payloadToEncrypt)

            // Write chunk: length (4 bytes) + isLast flag (1 byte) + ciphertext
            output.writeInt(ciphertextAndTag.size)
            output.writeByte(if (isLast) 1 else 0)
            output.write(ciphertextAndTag)

            bytesProcessed += plainBytes.size
            onProgress?.invoke(bytesProcessed, totalBytes)

            if (isLast) break
            chunkIndex++
        } while (true)

        output.flush()
    }

    override fun readStreamHeader(input: BufferedSource): StreamHeader {
        if (input.exhausted()) throw IOException("Empty ciphertext stream")
        val version = input.readByte()
        if (version != 1.toByte() && version != 2.toByte()) {
            throw IOException("Unsupported stream version: $version")
        }
        val noncePrefix = input.readByteArray(STREAM_NONCE_PREFIX_SIZE.toLong())
        return StreamHeader(version, noncePrefix)
    }

    override fun decryptStream(input: BufferedSource, output: BufferedSink, fileKey: ByteArray) {
        val header = readStreamHeader(input)
        val version = header.version
        val streamNoncePrefix = header.noncePrefix
        var chunkIndex = 0L
        var foundLastChunk = false

        while (!input.exhausted()) {
            val chunkLength = input.readInt()
            if (chunkLength < CryptoConstants.TAG_SIZE_BYTES || chunkLength > MAX_CHUNK_LENGTH) {
                throw IOException("Corrupted chunk: invalid chunk length $chunkLength (expected ${CryptoConstants.TAG_SIZE_BYTES}..$MAX_CHUNK_LENGTH)")
            }
            val isLast = input.readByte().toInt() == 1
            val ciphertextAndTag = input.readByteArray(chunkLength.toLong())

            val nonce = buildNonce(streamNoncePrefix, chunkIndex)
            val aad = buildAad(chunkIndex, isLast)

            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(fileKey, "AES"),
                    GCMParameterSpec(CryptoConstants.TAG_SIZE_BYTES * 8, nonce)
                )
                cipher.updateAAD(aad)
                val decrypted = cipher.doFinal(ciphertextAndTag)
                
                val plaintext = if (version >= 2 && isLast) {
                    val buf = ByteBuffer.wrap(decrypted)
                    val actualLength = buf.getInt()
                    if (actualLength < 0 || actualLength > CHUNK_DATA_SIZE) {
                        throw IOException("Corrupted chunk: invalid padded length $actualLength")
                    }
                    val actualBytes = ByteArray(actualLength)
                    buf.get(actualBytes)
                    actualBytes
                } else {
                    decrypted
                }
                output.write(plaintext)
            } catch (e: GeneralSecurityException) {
                throw IOException("Corrupted chunk $chunkIndex or authentication failure", e)
            }

            if (isLast) {
                foundLastChunk = true
                break
            }
            chunkIndex++
        }

        if (!foundLastChunk) {
            throw IOException("Stream truncated: missing final chunk flag")
        }
        output.flush()
    }

    override fun decryptChunk(
        input: BufferedSource,
        fileKey: ByteArray,
        chunkIndex: Long,
        streamNoncePrefix: ByteArray,
        version: Byte
    ): ByteArray {
        val chunkLength = input.readInt()
        if (chunkLength < CryptoConstants.TAG_SIZE_BYTES || chunkLength > MAX_CHUNK_LENGTH) {
            throw IOException("Corrupted chunk: invalid chunk length $chunkLength (expected ${CryptoConstants.TAG_SIZE_BYTES}..$MAX_CHUNK_LENGTH)")
        }
        val isLast = input.readByte().toInt() == 1
        val ciphertextAndTag = input.readByteArray(chunkLength.toLong())

        val nonce = buildNonce(streamNoncePrefix, chunkIndex)
        val aad = buildAad(chunkIndex, isLast)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(fileKey, "AES"),
            GCMParameterSpec(CryptoConstants.TAG_SIZE_BYTES * 8, nonce)
        )
        cipher.updateAAD(aad)
        val decrypted = cipher.doFinal(ciphertextAndTag)

        return if (version >= 2 && isLast) {
            val buf = ByteBuffer.wrap(decrypted)
            val actualLength = buf.getInt()
            if (actualLength < 0 || actualLength > CHUNK_DATA_SIZE) {
                throw IOException("Corrupted chunk: invalid padded length $actualLength")
            }
            val actualBytes = ByteArray(actualLength)
            buf.get(actualBytes)
            actualBytes
        } else {
            decrypted
        }
    }

    private fun buildNonce(prefix: ByteArray, index: Long): ByteArray {
        val nonce = ByteArray(CryptoConstants.NONCE_SIZE_BYTES)
        System.arraycopy(prefix, 0, nonce, 0, STREAM_NONCE_PREFIX_SIZE)
        // Put last 4 bytes as chunk index
        val indexInt = index.toInt()
        nonce[8] = (indexInt ushr 24).toByte()
        nonce[9] = (indexInt ushr 16).toByte()
        nonce[10] = (indexInt ushr 8).toByte()
        nonce[11] = indexInt.toByte()
        return nonce
    }

    private fun buildAad(index: Long, isLast: Boolean): ByteArray {
        val buffer = ByteBuffer.allocate(9)
        buffer.putLong(index)
        buffer.put((if (isLast) 1 else 0).toByte())
        return buffer.array()
    }
}
