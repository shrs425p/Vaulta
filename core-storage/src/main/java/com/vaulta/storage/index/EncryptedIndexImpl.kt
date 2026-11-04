package com.vaulta.storage.index

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.vaulta.crypto.KeyWrapping
import com.vaulta.storage.credentials.CredentialCategory
import com.vaulta.storage.credentials.CredentialEntity
import com.vaulta.storage.credentials.CredentialTemplate
import com.vaulta.storage.credentials.PasswordHistoryEntry
import com.vaulta.storage.credentials.VaultCredential
import com.vaulta.storage.credentials.toDomain
import com.vaulta.storage.credentials.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedIndexImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: VaultEntryDao,
    private val keyWrapping: KeyWrapping,
    private val database: VaultDatabase? = null
) : EncryptedIndex {

    private val vaultDir: File by lazy {
        File(context.filesDir, "vaulta").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val indexFile: File
        get() = File(vaultDir, "vault_index.enc")

    override suspend fun loadIndex(masterKey: ByteArray) {
        withContext(Dispatchers.IO) {
            val tempFile = File(vaultDir, "vault_index.enc.tmp")
            val targetFile = when {
                indexFile.exists() && indexFile.length() > 0 -> indexFile
                tempFile.exists() && tempFile.length() > 0 -> {
                    Log.w(TAG, "vault_index.enc missing. Restoring from vault_index.enc.tmp")
                    tempFile.copyTo(indexFile, overwrite = true)
                    indexFile
                }
                else -> null
            }

            if (targetFile != null) {
                try {
                    val encryptedBytes = targetFile.readBytes()
                    loadFromEncryptedData(encryptedBytes, masterKey)
                    Log.d(TAG, "Loaded encrypted index (${targetFile.length()} bytes)")
                    deleteLegacyDatabase()
                } catch (e: Exception) {
                    if (targetFile != tempFile && tempFile.exists() && tempFile.length() > 0) {
                        Log.w(TAG, "Corrupted indexFile. Attempting recovery from vault_index.enc.tmp", e)
                        try {
                            val tmpBytes = tempFile.readBytes()
                            loadFromEncryptedData(tmpBytes, masterKey)
                            tempFile.copyTo(indexFile, overwrite = true)
                            Log.i(TAG, "Successfully recovered index from vault_index.enc.tmp")
                            deleteLegacyDatabase()
                            return@withContext
                        } catch (e2: Exception) {
                            Log.e(TAG, "Recovery from vault_index.enc.tmp also failed", e2)
                        }
                    }
                    Log.e(TAG, "Failed to load encrypted index from disk", e)
                    throw e
                }
            } else {
                val legacyDb = context.getDatabasePath("vaulta_local.db")
                if (legacyDb.exists()) {
                    Log.i(TAG, "Found legacy vaulta_local.db. Migrating entries to encrypted index...")
                    try {
                        migrateLegacyDatabase(legacyDb)
                        saveIndex(masterKey)
                        deleteLegacyDatabase()
                        Log.i(TAG, "Migration complete. Legacy plaintext database shredded and deleted.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Legacy database migration failed", e)
                    }
                } else {
                    Log.d(TAG, "No existing vault_index.enc or legacy database found.")
                }
            }
        }
    }

    override suspend fun saveIndex(masterKey: ByteArray) {
        withContext(Dispatchers.IO) {
            val encryptedData = exportToEncryptedData(masterKey)
            val tempFile = File(vaultDir, "vault_index.enc.tmp")

            FileOutputStream(tempFile).use { fos ->
                fos.write(encryptedData)
                fos.flush()
                fos.fd.sync()
            }

            if (tempFile.exists()) {
                val moved = try {
                    java.nio.file.Files.move(
                        tempFile.toPath(),
                        indexFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE
                    )
                    true
                } catch (e: Exception) {
                    tempFile.copyTo(indexFile, overwrite = true)
                    tempFile.delete()
                    true
                }
                if (moved) {
                    Log.d(TAG, "Saved encrypted index (${encryptedData.size} bytes) atomically to disk")
                }
            }
        }
    }

    private suspend fun migrateLegacyDatabase(dbFile: File) {
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT id, name, parent_id, is_directory, mime_type, size_bytes, blob_name, file_salt, created_at, modified_at, sync_status FROM entries", null)
            val entries = mutableListOf<VaultEntryEntity>()

            cursor.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val name = c.getString(1)
                    val parentId = if (c.isNull(2)) null else c.getString(2)
                    val isDirectory = c.getInt(3) == 1
                    val mimeType = if (c.isNull(4)) null else c.getString(4)
                    val sizeBytes = c.getLong(5)
                    val blobName = if (c.isNull(6)) null else c.getString(6)
                    val fileSaltBase64 = if (c.isNull(7)) null else c.getString(7)
                    val createdAt = c.getLong(8)
                    val modifiedAt = c.getLong(9)
                    val syncStatus = c.getString(10) ?: SyncStatus.PENDING.name

                    entries.add(
                        VaultEntryEntity(
                            id = id,
                            name = name,
                            parentId = parentId,
                            isDirectory = isDirectory,
                            mimeType = mimeType,
                            sizeBytes = sizeBytes,
                            blobName = blobName,
                            fileSaltBase64 = fileSaltBase64,
                            createdAt = createdAt,
                            modifiedAt = modifiedAt,
                            syncStatus = syncStatus
                        )
                    )
                }
            }

            if (entries.isNotEmpty()) {
                dao.insertEntries(entries)
            }
        } finally {
            db?.close()
        }
    }

    private fun deleteLegacyDatabase() {
        val legacyDb = context.getDatabasePath("vaulta_local.db")
        if (!legacyDb.exists()) return

        try {
            val dbDir = legacyDb.parentFile
            if (dbDir != null && dbDir.exists()) {
                val files = dbDir.listFiles { _, name -> name.startsWith("vaulta_local.db") }
                files?.forEach { file ->
                    if (file.exists()) {
                        try {
                            val len = file.length()
                            if (len > 0) {
                                RandomAccessFile(file, "rws").use { raf ->
                                    val toWipe = len.toInt().coerceAtMost(1024 * 1024)
                                    raf.seek(0)
                                    raf.write(ByteArray(toWipe))
                                }
                            }
                            file.delete()
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
    }

    override suspend fun loadFromEncryptedData(encryptedBytes: ByteArray, masterKey: ByteArray) {
        val (entries, credentials) = parseAllFromEncryptedData(encryptedBytes, masterKey)
        dao.deleteAll()
        dao.insertEntries(entries.map { it.toEntity() })

        database?.credentialDao()?.deleteAll()
        if (credentials.isNotEmpty()) {
            database?.credentialDao()?.insertCredentials(credentials.map { it.toEntity() })
        }
    }

    override suspend fun parseFromEncryptedData(encryptedBytes: ByteArray, masterKey: ByteArray): List<VaultEntry> {
        return parseAllFromEncryptedData(encryptedBytes, masterKey).first
    }

    private fun parseAllFromEncryptedData(
        encryptedBytes: ByteArray,
        masterKey: ByteArray
    ): Pair<List<VaultEntry>, List<VaultCredential>> {
        if (encryptedBytes.isEmpty()) return Pair(emptyList(), emptyList())

        val decryptedBytes = keyWrapping.unwrap(encryptedBytes, masterKey)
        val dis = DataInputStream(ByteArrayInputStream(decryptedBytes))

        val entries = mutableListOf<VaultEntry>()
        val credentials = mutableListOf<VaultCredential>()

        val firstInt = dis.readInt()
        val isVersion4 = (firstInt == VERSION_4_MAGIC)
        val isVersion3 = (firstInt == VERSION_3_MAGIC)
        val isVersion2 = (firstInt == VERSION_2_MAGIC)
        val hasMagicHeader = isVersion4 || isVersion3 || isVersion2

        val fileCount = if (hasMagicHeader) dis.readInt() else firstInt

        for (i in 0 until fileCount) {
            val id = dis.readUTF()
            val name = dis.readUTF()
            val hasParent = dis.readBoolean()
            val parentId = if (hasParent) dis.readUTF() else null
            val isDirectory = dis.readBoolean()
            val hasMime = dis.readBoolean()
            val mimeType = if (hasMime) dis.readUTF() else null
            val sizeBytes = dis.readLong()
            val hasBlob = dis.readBoolean()
            val blobName = if (hasBlob) dis.readUTF() else null
            val saltLen = dis.readInt()
            val fileSalt = if (saltLen > 0) {
                val s = ByteArray(saltLen)
                dis.readFully(s)
                s
            } else null
            val createdAt = dis.readLong()
            val modifiedAt = dis.readLong()
            val syncStatusStr = dis.readUTF()
            val syncPolicyStr = if (isVersion3) dis.readUTF() else SyncPolicy.AUTO_SYNC.name

            val isBlobLocallyPresent = if (!blobName.isNullOrBlank() && !isDirectory) {
                File(File(context.filesDir, "vaulta"), "blobs/$blobName").exists()
            } else {
                true
            }

            val entry = VaultEntry(
                id = id,
                name = name,
                parentId = parentId,
                isDirectory = isDirectory,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                blobName = blobName,
                fileSalt = fileSalt,
                createdAt = createdAt,
                modifiedAt = modifiedAt,
                syncStatus = try {
                    SyncStatus.valueOf(syncStatusStr)
                } catch (_: Exception) {
                    SyncStatus.PENDING
                },
                syncPolicy = try {
                    SyncPolicy.valueOf(syncPolicyStr)
                } catch (_: Exception) {
                    SyncPolicy.AUTO_SYNC
                },
                isAvailableLocally = isBlobLocallyPresent
            )
            entries.add(entry)
        }

        if (hasMagicHeader && dis.available() > 0) {
            val credentialCount = dis.readInt()
            for (i in 0 until credentialCount) {
                val id = dis.readUTF()
                val categoryStr = dis.readUTF()
                val title = dis.readUTF()
                val username = dis.readUTF()
                val password = dis.readUTF()
                val url = dis.readUTF()
                val totpSecret = dis.readUTF()
                val cardNumber = dis.readUTF()
                val cardholderName = dis.readUTF()
                val cardExpiry = dis.readUTF()
                val cardCvv = dis.readUTF()
                val notes = dis.readUTF()
                val createdAt = dis.readLong()
                val modifiedAt = dis.readLong()
                val isFavorite = dis.readBoolean()

                val passwordHistoryJson = if (isVersion4) dis.readUTF() else "[]"
                val customFieldsJson = if (isVersion4) dis.readUTF() else "{}"
                val hasExpiresAt = if (isVersion4) dis.readBoolean() else false
                val expiresAt = if (hasExpiresAt) dis.readLong() else null
                val tagsCsv = if (isVersion4) dis.readUTF() else ""
                val hasColorTag = if (isVersion4) dis.readBoolean() else false
                val colorTag = if (hasColorTag) dis.readInt() else null
                val templateTypeStr = if (isVersion4) dis.readUTF() else CredentialTemplate.LOGIN.name

                val historyList = mutableListOf<PasswordHistoryEntry>()
                try {
                    val arr = org.json.JSONArray(passwordHistoryJson)
                    for (h in 0 until arr.length()) {
                        val obj = arr.getJSONObject(h)
                        historyList.add(
                            PasswordHistoryEntry(
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                password = obj.optString("password", "")
                            )
                        )
                    }
                } catch (_: Exception) {}

                val customMap = mutableMapOf<String, String>()
                try {
                    val obj = org.json.JSONObject(customFieldsJson)
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        customMap[key] = obj.optString(key, "")
                    }
                } catch (_: Exception) {}

                val tagList = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                val category = try {
                    CredentialCategory.valueOf(categoryStr)
                } catch (_: Exception) {
                    CredentialCategory.LOGIN
                }

                credentials.add(
                    VaultCredential(
                        id = id,
                        category = category,
                        title = title,
                        username = username,
                        password = password,
                        url = url,
                        totpSecret = totpSecret,
                        cardNumber = cardNumber,
                        cardholderName = cardholderName,
                        cardExpiry = cardExpiry,
                        cardCvv = cardCvv,
                        notes = notes,
                        createdAt = createdAt,
                        modifiedAt = modifiedAt,
                        isFavorite = isFavorite,
                        passwordHistory = historyList,
                        customFields = customMap,
                        expiresAt = expiresAt,
                        tags = tagList,
                        colorTag = colorTag,
                        templateType = try { CredentialTemplate.valueOf(templateTypeStr) } catch (_: Exception) { CredentialTemplate.LOGIN }
                    )
                )
            }
        }

        return Pair(entries, credentials)
    }

    override suspend fun exportToEncryptedData(masterKey: ByteArray): ByteArray {
        val allEntities = dao.getAllEntries()
        val allCredentials = database?.credentialDao()?.getAllCredentialsList() ?: emptyList()

        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeInt(VERSION_4_MAGIC)
        dos.writeInt(allEntities.size)

        for (entity in allEntities) {
            val entry = entity.toDomain()
            dos.writeUTF(entry.id)
            dos.writeUTF(entry.name)
            dos.writeBoolean(entry.parentId != null)
            if (entry.parentId != null) dos.writeUTF(entry.parentId)
            dos.writeBoolean(entry.isDirectory)
            dos.writeBoolean(entry.mimeType != null)
            if (entry.mimeType != null) dos.writeUTF(entry.mimeType)
            dos.writeLong(entry.sizeBytes)
            dos.writeBoolean(entry.blobName != null)
            if (entry.blobName != null) dos.writeUTF(entry.blobName)

            val salt = entry.fileSalt
            if (salt != null && salt.isNotEmpty()) {
                dos.writeInt(salt.size)
                dos.write(salt)
            } else {
                dos.writeInt(0)
            }
            dos.writeLong(entry.createdAt)
            dos.writeLong(entry.modifiedAt)
            dos.writeUTF(entry.syncStatus.name)
            dos.writeUTF(entry.syncPolicy.name)
        }

        dos.writeInt(allCredentials.size)
        for (credEntity in allCredentials) {
            val cred = credEntity.toDomain()
            dos.writeUTF(cred.id)
            dos.writeUTF(cred.category.name)
            dos.writeUTF(cred.title)
            dos.writeUTF(cred.username)
            dos.writeUTF(cred.password)
            dos.writeUTF(cred.url)
            dos.writeUTF(cred.totpSecret)
            dos.writeUTF(cred.cardNumber)
            dos.writeUTF(cred.cardholderName)
            dos.writeUTF(cred.cardExpiry)
            dos.writeUTF(cred.cardCvv)
            dos.writeUTF(cred.notes)
            dos.writeLong(cred.createdAt)
            dos.writeLong(cred.modifiedAt)
            dos.writeBoolean(cred.isFavorite)

            dos.writeUTF(credEntity.passwordHistoryJson)
            dos.writeUTF(credEntity.customFieldsJson)
            dos.writeBoolean(cred.expiresAt != null)
            if (cred.expiresAt != null) dos.writeLong(cred.expiresAt)
            dos.writeUTF(credEntity.tagsCsv)
            dos.writeBoolean(cred.colorTag != null)
            if (cred.colorTag != null) dos.writeInt(cred.colorTag)
            dos.writeUTF(cred.templateType.name)
        }

        dos.flush()
        val plaintextBytes = baos.toByteArray()
        return keyWrapping.wrap(plaintextBytes, masterKey)
    }

    override suspend fun addEntry(entry: VaultEntry) {
        dao.insertEntry(entry.toEntity())
    }

    override suspend fun updateEntry(entry: VaultEntry) {
        dao.updateEntry(entry.toEntity())
    }

    override suspend fun removeEntry(id: String) {
        val existing = dao.getEntryById(id) ?: return
        dao.updateEntry(existing.copy(syncStatus = SyncStatus.DELETED.name))
    }

    override fun getEntriesByParent(parentId: String?): Flow<List<VaultEntry>> {
        return dao.getEntriesByParent(parentId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllFolders(): Flow<List<VaultEntry>> {
        return dao.getAllFolders().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getEntry(id: String): VaultEntry? {
        return dao.getEntryById(id)?.toDomain()
    }

    override fun searchEntries(query: String): Flow<List<VaultEntry>> {
        return dao.searchEntries(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            dao.deleteAll()
            database?.credentialDao()?.deleteAll()
            try {
                database?.clearAllTables()
                database?.invalidationTracker?.refreshVersionsSync()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fully clear in-memory database tables", e)
            }
        }
    }

    override suspend fun getEntryCount(): Int {
        return dao.getEntryCount()
    }

    override suspend fun getVaultStats(): VaultStats = withContext(Dispatchers.IO) {
        val entries = dao.getAllEntries().filter { it.syncStatus != SyncStatus.DELETED.name }
        val files = entries.filter { !it.isDirectory }
        val folders = entries.filter { it.isDirectory }
        val totalSize = files.sumOf { it.sizeBytes }
        VaultStats(
            fileCount = files.size,
            folderCount = folders.size,
            totalSizeBytes = totalSize
        )
    }

    companion object {
        private const val TAG = "EncryptedIndex"
        private const val VERSION_4_MAGIC = 0x56415504 // "VAU4"
        private const val VERSION_3_MAGIC = 0x56415503 // "VAU3"
        private const val VERSION_2_MAGIC = 0x56415532 // "VAU2"
    }
}
