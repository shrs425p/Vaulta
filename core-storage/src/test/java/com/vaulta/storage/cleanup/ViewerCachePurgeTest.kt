package com.vaulta.storage.cleanup

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ViewerCachePurgeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var fakeCacheDir: File

    @Before
    fun setUp() {
        fakeCacheDir = tempFolder.newFolder("cache")
        mockContext = mockk<Context>()
        every { mockContext.cacheDir } returns fakeCacheDir
    }

    @Test
    fun purgeViewerTemp_deletesFilesAndDirectoriesInViewerTempAndSharedTemp() {
        val viewerTemp = File(fakeCacheDir, "viewer_temp").apply { mkdirs() }
        val sharedTemp = File(fakeCacheDir, "shared_temp").apply { mkdirs() }

        val vidFile = File(viewerTemp, "vid_sample.mp4").apply {
            writeBytes("decrypted sensitive video data".toByteArray())
        }
        val subDir = File(viewerTemp, "subfolder").apply { mkdirs() }
        val nestedFile = File(subDir, "nested_doc.pdf").apply {
            writeBytes("decrypted sensitive nested pdf data".toByteArray())
        }
        val sharedFile = File(sharedTemp, "shared_pic.jpg").apply {
            writeBytes("decrypted sensitive photo data".toByteArray())
        }

        assertThat(vidFile.exists()).isTrue()
        assertThat(nestedFile.exists()).isTrue()
        assertThat(sharedFile.exists()).isTrue()

        ViewerCachePurge.purgeViewerTemp(mockContext)

        assertThat(vidFile.exists()).isFalse()
        assertThat(nestedFile.exists()).isFalse()
        assertThat(viewerTemp.exists()).isFalse()
        assertThat(sharedFile.exists()).isFalse()
        assertThat(sharedTemp.exists()).isFalse()
    }

    @Test
    fun purgeViewerTemp_whenDirsDoNotExist_runsWithoutException() {
        // No viewer_temp or shared_temp created
        ViewerCachePurge.purgeViewerTemp(mockContext)

        assertThat(File(fakeCacheDir, "viewer_temp").exists()).isFalse()
        assertThat(File(fakeCacheDir, "shared_temp").exists()).isFalse()
    }
}
