package com.vaulta.crypto

import com.google.common.truth.Truth.assertThat
import okio.Buffer
import okio.source
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.IOException

class StreamingEncryptionTest {

    private lateinit var streamingEncryption: StreamingEncryption
    private lateinit var randomProvider: SecureRandomProvider

    @Before
    fun setUp() {
        randomProvider = SecureRandomProviderImpl()
        streamingEncryption = StreamingEncryptionImpl(randomProvider)
    }

    @Test
    fun roundTrip_encryptsAndDecrypts_smallData() {
        val fileKey = randomProvider.nextBytes(32)
        val originalData = "Hello, Vaulta secure encrypted streaming!".toByteArray()

        val input = Buffer().write(originalData)
        val encryptedOutput = Buffer()
        streamingEncryption.encryptStream(input, encryptedOutput, fileKey)

        val decryptedOutput = Buffer()
        streamingEncryption.decryptStream(encryptedOutput, decryptedOutput, fileKey)

        assertThat(decryptedOutput.readByteArray()).isEqualTo(originalData)
    }

    @Test
    fun roundTrip_encryptsAndDecrypts_multiChunkData() {
        val fileKey = randomProvider.nextBytes(32)
        // Data larger than 1 chunk (64KB + 10KB = 74KB)
        val originalData = ByteArray(74 * 1024) { (it % 256).toByte() }

        val input = Buffer().write(originalData)
        val encryptedOutput = Buffer()
        streamingEncryption.encryptStream(input, encryptedOutput, fileKey)

        val decryptedOutput = Buffer()
        streamingEncryption.decryptStream(encryptedOutput, decryptedOutput, fileKey)

        assertThat(decryptedOutput.readByteArray()).isEqualTo(originalData)
    }

    @Test
    fun roundTrip_encryptsAndDecrypts_largeMultiChunkFromInputStream() {
        val fileKey = randomProvider.nextBytes(32)
        // 5 MB of test data (spanning 80 chunks of 64KB)
        val originalData = ByteArray(5 * 1024 * 1024) { (it % 251).toByte() }

        val inputStream = originalData.inputStream()
        val encryptedOutput = Buffer()
        streamingEncryption.encryptStream(inputStream.source(), encryptedOutput, fileKey)

        val decryptedOutput = Buffer()
        streamingEncryption.decryptStream(encryptedOutput, decryptedOutput, fileKey)

        val decryptedBytes = decryptedOutput.readByteArray()
        assertThat(decryptedBytes.size).isEqualTo(originalData.size)
        assertThat(decryptedBytes).isEqualTo(originalData)
    }

    @Test
    fun decrypt_detectsTruncation() {
        val fileKey = randomProvider.nextBytes(32)
        val originalData = ByteArray(128 * 1024) { 0x42 }

        val input = Buffer().write(originalData)
        val encryptedOutput = Buffer()
        streamingEncryption.encryptStream(input, encryptedOutput, fileKey)

        // Truncate the buffer by dropping the last 500 bytes
        val truncatedBytes = encryptedOutput.readByteArray(encryptedOutput.size - 500)
        val truncatedBuffer = Buffer().write(truncatedBytes)

        val decryptedOutput = Buffer()
        assertThrows(IOException::class.java) {
            streamingEncryption.decryptStream(truncatedBuffer, decryptedOutput, fileKey)
        }
    }

    @Test
    fun trafficPadding_finalChunkIsPaddedToStandardSize() {
        val fileKey = randomProvider.nextBytes(32)
        val data10Bytes = ByteArray(10) { 1 }
        val data500Bytes = ByteArray(500) { 2 }

        val out1 = Buffer()
        streamingEncryption.encryptStream(Buffer().write(data10Bytes), out1, fileKey)

        val out2 = Buffer()
        streamingEncryption.encryptStream(Buffer().write(data500Bytes), out2, fileKey)

        // Both single-chunk files under 64KB result in the EXACT same encrypted stream size
        // due to random padding to standard 64KB chunk boundaries!
        assertThat(out1.size).isEqualTo(out2.size)

        val dec1 = Buffer()
        streamingEncryption.decryptStream(out1, dec1, fileKey)
        assertThat(dec1.readByteArray()).isEqualTo(data10Bytes)

        val dec2 = Buffer()
        streamingEncryption.decryptStream(out2, dec2, fileKey)
        assertThat(dec2.readByteArray()).isEqualTo(data500Bytes)
    }

    @Test
    fun decryptChunk_withReadStreamHeader_decryptsCorrectly() {
        val fileKey = randomProvider.nextBytes(32)
        val data = "Chunk decryption test payload".toByteArray()

        val encryptedOutput = Buffer()
        streamingEncryption.encryptStream(Buffer().write(data), encryptedOutput, fileKey)

        val header = streamingEncryption.readStreamHeader(encryptedOutput)
        assertThat(header.version).isEqualTo(StreamingEncryptionImpl.STREAM_VERSION)
        assertThat(header.noncePrefix).hasLength(StreamingEncryptionImpl.STREAM_NONCE_PREFIX_SIZE)

        val decryptedChunk = streamingEncryption.decryptChunk(
            input = encryptedOutput,
            fileKey = fileKey,
            chunkIndex = 0L,
            streamNoncePrefix = header.noncePrefix,
            version = header.version
        )
        assertThat(decryptedChunk).isEqualTo(data)
    }

    @Test
    fun decryptStream_rejectsMalformedChunkLength_tooSmall() {
        val fileKey = randomProvider.nextBytes(32)
        val malformedBuffer = Buffer().apply {
            writeByte(StreamingEncryptionImpl.STREAM_VERSION.toInt())
            write(randomProvider.nextBytes(StreamingEncryptionImpl.STREAM_NONCE_PREFIX_SIZE))
            writeInt(5) // Less than TAG_SIZE_BYTES (16)
            writeByte(1) // isLast
            write(ByteArray(5))
        }

        val decryptedOutput = Buffer()
        assertThrows(IOException::class.java) {
            streamingEncryption.decryptStream(malformedBuffer, decryptedOutput, fileKey)
        }
    }

    @Test
    fun decryptStream_rejectsMalformedChunkLength_tooLarge() {
        val fileKey = randomProvider.nextBytes(32)
        val malformedBuffer = Buffer().apply {
            writeByte(StreamingEncryptionImpl.STREAM_VERSION.toInt())
            write(randomProvider.nextBytes(StreamingEncryptionImpl.STREAM_NONCE_PREFIX_SIZE))
            writeInt(1_000_000) // Exceeds MAX_CHUNK_LENGTH (65,556)
            writeByte(1) // isLast
        }

        val decryptedOutput = Buffer()
        assertThrows(IOException::class.java) {
            streamingEncryption.decryptStream(malformedBuffer, decryptedOutput, fileKey)
        }
    }
}
