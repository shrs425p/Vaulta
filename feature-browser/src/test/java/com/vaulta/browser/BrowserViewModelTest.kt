package com.vaulta.browser

import com.google.common.truth.Truth.assertThat
import com.vaulta.crypto.KeyDerivation
import com.vaulta.storage.blob.BlobStore
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.storage.index.SyncStatus
import com.vaulta.storage.index.VaultEntry
import com.vaulta.storage.saf.SafAccessor
import com.vaulta.vault.session.VaultSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val encryptedIndex = mockk<EncryptedIndex>(relaxed = true)
    private val blobStore = mockk<BlobStore>(relaxed = true)
    private val safAccessor = mockk<SafAccessor>(relaxed = true)
    private val keyDerivation = mockk<KeyDerivation>(relaxed = true)
    private val vaultSession = mockk<VaultSession>(relaxed = true)

    private val indexEntries = mutableListOf<VaultEntry>()
    private val entriesFlow = MutableStateFlow<List<VaultEntry>>(emptyList())
    private val allFoldersFlow = MutableStateFlow<List<VaultEntry>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        indexEntries.clear()
        entriesFlow.value = emptyList()

        every { vaultSession.isUnlocked } returns MutableStateFlow(true)
        every { vaultSession.getMasterKey() } returns ByteArray(32) { 0x42 }

        coEvery { encryptedIndex.getEntriesByParent(any()) } answers {
            val parent = firstArg<String?>()
            entriesFlow.value = indexEntries.filter { it.parentId == parent }
            entriesFlow
        }
        coEvery { encryptedIndex.getAllFolders() } returns allFoldersFlow
        coEvery { encryptedIndex.addEntry(any()) } answers {
            val entry = firstArg<VaultEntry>()
            indexEntries.add(entry)
            entriesFlow.value = indexEntries.toList()
        }
        coEvery { encryptedIndex.updateEntry(any()) } answers {
            val entry = firstArg<VaultEntry>()
            val idx = indexEntries.indexOfFirst { it.id == entry.id }
            if (idx >= 0) {
                indexEntries[idx] = entry
            }
            entriesFlow.value = indexEntries.toList()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): BrowserViewModel {
        val appPreferencesManager = mockk<com.vaulta.vault.preferences.AppPreferencesManager>(relaxed = true)
        val syncProvider = mockk<com.vaulta.vault.sync.SyncProvider>(relaxed = true)
        return BrowserViewModel(
            encryptedIndex = encryptedIndex,
            blobStore = blobStore,
            safAccessor = safAccessor,
            keyDerivation = keyDerivation,
            appPreferencesManager = appPreferencesManager,
            syncProvider = syncProvider,
            vaultSession = vaultSession
        ).apply {
            ioDispatcher = testDispatcher
        }
    }

    private fun makeFile(name: String, parentId: String? = null, mime: String? = null, size: Long = 1000L): VaultEntry {
        return VaultEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            parentId = parentId,
            isDirectory = false,
            mimeType = mime,
            sizeBytes = size,
            blobName = "blob_${UUID.randomUUID()}",
            fileSalt = ByteArray(16),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED
        )
    }

    private fun makeFolder(name: String, parentId: String? = null): VaultEntry {
        return VaultEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            parentId = parentId,
            isDirectory = true,
            mimeType = null,
            sizeBytes = 0L,
            blobName = null,
            fileSalt = null,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED
        )
    }

    @Test
    fun categorizeEntry_correctlyClassifiesAllCategories() {
        assertThat(BrowserViewModel.categorizeEntry(makeFile("photo.jpg"))).isEqualTo("Images")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("screenshot.png"))).isEqualTo("Images")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("movie.mp4"))).isEqualTo("Videos")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("recording.mkv"))).isEqualTo("Videos")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("song.mp3"))).isEqualTo("Audio")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("voice_note.m4a"))).isEqualTo("Audio")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("report.pdf"))).isEqualTo("Documents")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("spreadsheet.xlsx"))).isEqualTo("Documents")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("finance.csv", mime = "text/csv"))).isEqualTo("Documents")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("data.tsv"))).isEqualTo("Documents")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("readme.txt"))).isEqualTo("Text Documents")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("notes.md"))).isEqualTo("Text Documents")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("script.py"))).isEqualTo("Code & Scripts")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("config.json"))).isEqualTo("Code & Scripts")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("backup.zip"))).isEqualTo("Archives")
        assertThat(BrowserViewModel.categorizeEntry(makeFile("archive.tar.gz"))).isEqualTo("Archives")
        assertThat(BrowserViewModel.categorizeEntry(makeFolder("My Folder"))).isNull()
    }

    @Test
    fun matchesTypeFilter_filtersCorrectly() {
        val image = makeFile("pic.png")
        val video = makeFile("clip.mp4")
        val audio = makeFile("podcast.mp3")
        val doc = makeFile("paper.pdf")
        val csv = makeFile("data.csv", mime = "text/csv")
        val text = makeFile("notes.txt")
        val code = makeFile("main.kt")
        val archive = makeFile("data.7z")

        assertThat(BrowserViewModel.matchesTypeFilter(image, FileTypeFilter.IMAGES)).isTrue()
        assertThat(BrowserViewModel.matchesTypeFilter(video, FileTypeFilter.IMAGES)).isFalse()

        assertThat(BrowserViewModel.matchesTypeFilter(video, FileTypeFilter.VIDEOS)).isTrue()
        assertThat(BrowserViewModel.matchesTypeFilter(audio, FileTypeFilter.AUDIO)).isTrue()
        assertThat(BrowserViewModel.matchesTypeFilter(doc, FileTypeFilter.DOCUMENTS)).isTrue()
        assertThat(BrowserViewModel.matchesTypeFilter(csv, FileTypeFilter.DOCUMENTS)).isTrue()
        assertThat(BrowserViewModel.matchesTypeFilter(csv, FileTypeFilter.TEXT)).isFalse()
        assertThat(BrowserViewModel.matchesTypeFilter(text, FileTypeFilter.TEXT)).isTrue()
        assertThat(BrowserViewModel.matchesTypeFilter(code, FileTypeFilter.CODE)).isTrue()
        assertThat(BrowserViewModel.matchesTypeFilter(archive, FileTypeFilter.ARCHIVES)).isTrue()
    }

    @Test
    fun autoOrganize_movesFilesToCategoryFoldersAndSavesIndex() = runTest {
        val file1 = makeFile("vacation.jpg")
        val file2 = makeFile("budget.xlsx")
        val file3 = makeFile("tutorial.mp4")
        val file4 = makeFile("readme.txt")
        val file5 = makeFile("server.py")
        val file6 = makeFile("archive.zip")

        indexEntries.addAll(listOf(file1, file2, file3, file4, file5, file6))

        val viewModel = createViewModel()
        viewModel.autoOrganize(null)
        advanceUntilIdle()

        // Index should contain 6 category folders + 6 moved files
        val folders = indexEntries.filter { it.isDirectory }
        val folderNames = folders.map { it.name }.toSet()
        assertThat(folderNames).containsExactly(
            "Images", "Documents", "Videos", "Text Documents", "Code & Scripts", "Archives"
        )

        // Verify each file's parentId points to its corresponding category folder
        val imagesFolder = folders.first { it.name == "Images" }
        val updatedFile1 = indexEntries.first { it.id == file1.id }
        assertThat(updatedFile1.parentId).isEqualTo(imagesFolder.id)

        val docsFolder = folders.first { it.name == "Documents" }
        val updatedFile2 = indexEntries.first { it.id == file2.id }
        assertThat(updatedFile2.parentId).isEqualTo(docsFolder.id)

        // Verify saveIndex was called with master key
        coVerify(atLeast = 1) { encryptedIndex.saveIndex(any()) }

        assertThat(viewModel.userMessage.value).contains("Organized 6 files")
    }

    @Test
    fun autoOrganize_reusesExistingFoldersWithoutDuplicating() = runTest {
        val existingImagesFolder = makeFolder("Images")
        val existingDocsFolder = makeFolder("Documents")
        val file1 = makeFile("photo.png")
        val file2 = makeFile("contract.pdf")

        indexEntries.addAll(listOf(existingImagesFolder, existingDocsFolder, file1, file2))

        val viewModel = createViewModel()
        viewModel.autoOrganize(null)
        advanceUntilIdle()

        // Images and Documents folders should not be duplicated
        val imageFolders = indexEntries.filter { it.isDirectory && it.name.equals("Images", ignoreCase = true) }
        assertThat(imageFolders).hasSize(1)

        val docFolders = indexEntries.filter { it.isDirectory && it.name.equals("Documents", ignoreCase = true) }
        assertThat(docFolders).hasSize(1)

        val updatedFile1 = indexEntries.first { it.id == file1.id }
        assertThat(updatedFile1.parentId).isEqualTo(existingImagesFolder.id)
    }
}
