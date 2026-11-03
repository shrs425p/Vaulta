package com.vaulta.storage.index

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.vaulta.crypto.KeyUnwrapException
import com.vaulta.crypto.KeyWrapping
import com.vaulta.crypto.KeyWrappingImpl
import com.vaulta.crypto.SecureRandomProviderImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EncryptedIndexTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var fakeFilesDir: File
    private lateinit var fakeDao: FakeVaultEntryDao
    private lateinit var keyWrapping: KeyWrapping
    private lateinit var encryptedIndex: EncryptedIndexImpl
    private val masterKey = ByteArray(32) { 0x42.toByte() }

    @Before
    fun setUp() {
        fakeFilesDir = tempFolder.newFolder("files")
        mockContext = mockk<Context>()
        every { mockContext.filesDir } returns fakeFilesDir
        every { mockContext.getDatabasePath(any()) } returns File(fakeFilesDir, "vaulta_local.db")
        every { mockContext.deleteDatabase(any()) } returns true

        fakeDao = FakeVaultEntryDao()
        keyWrapping = KeyWrappingImpl(SecureRandomProviderImpl())
        encryptedIndex = EncryptedIndexImpl(mockContext, fakeDao, keyWrapping)
    }

    @Test
    fun saveIndex_and_loadIndex_restoresAllEntriesInMemory() = runTest {
        val folder = VaultEntry(
            id = "folder-1",
            name = "Documents",
            parentId = null,
            isDirectory = true,
            mimeType = null,
            sizeBytes = 0L,
            blobName = null,
            fileSalt = null,
            createdAt = 1000L,
            modifiedAt = 1000L,
            syncStatus = SyncStatus.SYNCED
        )
        val file = VaultEntry(
            id = "file-1",
            name = "secret.txt",
            parentId = "folder-1",
            isDirectory = false,
            mimeType = "text/plain",
            sizeBytes = 1024L,
            blobName = "blob-uuid-1234.blob",
            fileSalt = ByteArray(16) { 0x07 },
            createdAt = 2000L,
            modifiedAt = 2000L,
            syncStatus = SyncStatus.PENDING
        )

        encryptedIndex.addEntry(folder)
        encryptedIndex.addEntry(file)

        assertThat(fakeDao.getAllEntries()).hasSize(2)

        // Save index to encrypted disk file
        encryptedIndex.saveIndex(masterKey)

        val indexEncFile = File(fakeFilesDir, "vaulta/vault_index.enc")
        assertThat(indexEncFile.exists()).isTrue()
        assertThat(indexEncFile.length()).isGreaterThan(0L)

        // Verify that plaintext entry names NEVER appear in the encrypted index file on disk
        val rawFileBytes = indexEncFile.readBytes()
        val rawFileString = String(rawFileBytes, Charsets.ISO_8859_1)
        assertThat(rawFileString).doesNotContain("secret.txt")
        assertThat(rawFileString).doesNotContain("Documents")

        // Simulate locking/wiping in-memory state
        encryptedIndex.clear()
        assertThat(fakeDao.getAllEntries()).isEmpty()

        // Decrypt and reload from disk
        encryptedIndex.loadIndex(masterKey)

        val restoredEntries = fakeDao.getAllEntries()
        assertThat(restoredEntries).hasSize(2)

        val restoredFile = encryptedIndex.getEntry("file-1")
        assertThat(restoredFile).isNotNull()
        assertThat(restoredFile?.name).isEqualTo("secret.txt")
        assertThat(restoredFile?.parentId).isEqualTo("folder-1")
        assertThat(restoredFile?.blobName).isEqualTo("blob-uuid-1234.blob")
        assertThat(restoredFile?.fileSalt).isEqualTo(ByteArray(16) { 0x07 })
        assertThat(restoredFile?.syncStatus).isEqualTo(SyncStatus.PENDING)
    }

    @Test
    fun loadIndex_withWrongMasterKey_throwsKeyUnwrapException() = runTest {
        val entry = VaultEntry(
            id = "entry-1",
            name = "passwords.kdbx",
            parentId = null,
            isDirectory = false,
            mimeType = "application/octet-stream",
            sizeBytes = 500L,
            blobName = "blob-passwords.blob",
            fileSalt = ByteArray(16) { 0x01 },
            createdAt = 100L,
            modifiedAt = 100L,
            syncStatus = SyncStatus.PENDING
        )
        encryptedIndex.addEntry(entry)
        encryptedIndex.saveIndex(masterKey)

        val wrongKey = ByteArray(32) { 0x99.toByte() }
        assertThrows(KeyUnwrapException::class.java) {
            kotlinx.coroutines.runBlocking {
                encryptedIndex.loadIndex(wrongKey)
            }
        }
    }

    @Test
    fun clear_purgesAllEntriesFromInMemoryStore() = runTest {
        val entry = VaultEntry(
            id = "entry-1",
            name = "test.pdf",
            parentId = null,
            isDirectory = false,
            mimeType = "application/pdf",
            sizeBytes = 100L,
            blobName = null,
            fileSalt = null,
            createdAt = 10L,
            modifiedAt = 10L,
            syncStatus = SyncStatus.PENDING
        )
        encryptedIndex.addEntry(entry)
        assertThat(fakeDao.getAllEntries()).hasSize(1)

        encryptedIndex.clear()
        assertThat(fakeDao.getAllEntries()).isEmpty()
        assertThat(encryptedIndex.getEntryCount()).isEqualTo(0)
    }

    @Test
    fun loadIndex_recoversFromTempFile_ifMainFileMissing() = runTest {
        val entry = VaultEntry(
            id = "file-fallback",
            name = "recovered.txt",
            parentId = null,
            isDirectory = false,
            mimeType = "text/plain",
            sizeBytes = 123L,
            blobName = "blob-rec.blob",
            fileSalt = ByteArray(16) { 0x01 },
            createdAt = 100L,
            modifiedAt = 100L,
            syncStatus = SyncStatus.SYNCED
        )
        encryptedIndex.addEntry(entry)
        encryptedIndex.saveIndex(masterKey)

        val vaultDir = File(fakeFilesDir, "vaulta")
        val mainFile = File(vaultDir, "vault_index.enc")
        val tmpFile = File(vaultDir, "vault_index.enc.tmp")

        // Simulate crash: move mainFile to tmpFile so mainFile is missing
        mainFile.renameTo(tmpFile)
        assertThat(mainFile.exists()).isFalse()
        assertThat(tmpFile.exists()).isTrue()

        // Clear in-memory DB to verify loadIndex restores it from tmpFile
        fakeDao.deleteAll()
        assertThat(fakeDao.getAllEntries()).isEmpty()

        encryptedIndex.loadIndex(masterKey)
        val loaded = fakeDao.getAllEntries()
        assertThat(loaded).hasSize(1)
        assertThat(loaded.first().name).isEqualTo("recovered.txt")
    }
}

class FakeVaultEntryDao : VaultEntryDao {
    private val map = mutableMapOf<String, VaultEntryEntity>()

    override fun getEntriesByParent(parentId: String?): Flow<List<VaultEntryEntity>> {
        return flowOf(map.values.filter { it.parentId == parentId && it.syncStatus != "DELETED" })
    }

    override fun getAllFolders(): Flow<List<VaultEntryEntity>> {
        return flowOf(map.values.filter { it.isDirectory && it.syncStatus != "DELETED" })
    }

    override suspend fun getEntryById(id: String): VaultEntryEntity? {
        return map[id]
    }

    override fun searchEntries(query: String): Flow<List<VaultEntryEntity>> {
        return flowOf(map.values.filter { it.name.contains(query, ignoreCase = true) && it.syncStatus != "DELETED" })
    }

    override suspend fun insertEntry(entry: VaultEntryEntity) {
        map[entry.id] = entry
    }

    override suspend fun insertEntries(entries: List<VaultEntryEntity>) {
        for (e in entries) {
            map[e.id] = e
        }
    }

    override suspend fun updateEntry(entry: VaultEntryEntity) {
        map[entry.id] = entry
    }

    override suspend fun deleteEntry(entry: VaultEntryEntity) {
        map.remove(entry.id)
    }

    override suspend fun getEntriesBySyncStatus(status: String): List<VaultEntryEntity> {
        return map.values.filter { it.syncStatus == status }
    }

    override suspend fun getAllEntries(): List<VaultEntryEntity> {
        return map.values.toList()
    }

    override suspend fun deleteAll() {
        map.clear()
    }

    override suspend fun getEntryCount(): Int {
        return map.values.count { it.syncStatus != "DELETED" }
    }
}
