package com.vaulta.storage.blob

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.vaulta.crypto.SecureRandomProviderImpl
import com.vaulta.crypto.StreamingEncryptionImpl
import io.mockk.every
import io.mockk.mockk
import okio.Buffer
import okio.buffer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BlobStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var blobStore: BlobStoreImpl
    private val fileKey = ByteArray(32) { 0x33.toByte() }

    @Before
    fun setUp() {
        val fakeFilesDir = tempFolder.newFolder("files")
        mockContext = mockk<Context>()
        every { mockContext.filesDir } returns fakeFilesDir

        val streamingEncryption = StreamingEncryptionImpl(SecureRandomProviderImpl())
        blobStore = BlobStoreImpl(mockContext, streamingEncryption)
    }

    @Test
    fun writeBlob_and_readBlob_streamsCorrectContent() {
        val originalData = "Hello, streaming zero-knowledge storage!".repeat(100).toByteArray(Charsets.UTF_8)
        val inputSource = Buffer().write(originalData)

        blobStore.writeBlob(
            blobName = "test.blob",
            input = inputSource,
            fileKey = fileKey,
            totalBytes = originalData.size.toLong()
        )

        val streamingSource = blobStore.readBlob("test.blob", fileKey)
        val readBuffer = Buffer()
        streamingSource.buffer().use { it.readAll(readBuffer) }

        assertThat(readBuffer.readByteArray()).isEqualTo(originalData)
    }
}
