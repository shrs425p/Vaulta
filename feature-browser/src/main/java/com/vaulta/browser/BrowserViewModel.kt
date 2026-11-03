package com.vaulta.browser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulta.crypto.KeyDerivation
import com.vaulta.crypto.SecureMemory
import com.vaulta.storage.blob.BlobStore
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.storage.index.SyncStatus
import com.vaulta.storage.index.VaultEntry
import com.vaulta.storage.saf.SafAccessor
import com.vaulta.vault.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class ImportProgress(
    val fileName: String,
    val currentFileIndex: Int,
    val totalFiles: Int,
    val bytesProcessed: Long,
    val totalBytes: Long,
    val percentage: Int
)

enum class SortOption(val label: String, val shortLabel: String) {
    NAME_ASC("Name (A to Z)", "Name A-Z"),
    NAME_DESC("Name (Z to A)", "Name Z-A"),
    DATE_DESC("Date (Newest First)", "Newest"),
    DATE_ASC("Date (Oldest First)", "Oldest"),
    SIZE_DESC("Size (Largest First)", "Largest"),
    SIZE_ASC("Size (Smallest First)", "Smallest")
}

enum class FileTypeFilter(val label: String) {
    ALL("All"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    TEXT("Text"),
    CODE("Code"),
    ARCHIVES("Archives")
}

private data class FilterSortCriteria(
    val parentId: String?,
    val query: String,
    val sortOption: SortOption,
    val typeFilter: FileTypeFilter
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val encryptedIndex: EncryptedIndex,
    private val blobStore: BlobStore,
    private val safAccessor: SafAccessor,
    private val keyDerivation: KeyDerivation,
    val vaultSession: VaultSession
) : ViewModel() {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _currentParentId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _typeFilter = MutableStateFlow(FileTypeFilter.ALL)
    val typeFilter: StateFlow<FileTypeFilter> = _typeFilter.asStateFlow()

    val entries: StateFlow<List<VaultEntry>> = combine(
        _currentParentId,
        _searchQuery,
        _sortOption,
        _typeFilter
    ) { parentId, query, sort, filter ->
        FilterSortCriteria(parentId, query, sort, filter)
    }.flatMapLatest { criteria ->
        val baseFlow = if (criteria.query.isBlank()) {
            encryptedIndex.getEntriesByParent(criteria.parentId)
        } else {
            encryptedIndex.searchEntries(criteria.query)
        }
        baseFlow.map { list ->
            // 1. Filter
            val filtered = if (criteria.typeFilter == FileTypeFilter.ALL) {
                list
            } else {
                list.filter { entry ->
                    if (entry.isDirectory) {
                        false
                    } else {
                        matchesTypeFilter(entry, criteria.typeFilter)
                    }
                }
            }

            // 2. Sort: Folders always appear first, followed by files sorted by the chosen criteria
            val folders = filtered.filter { it.isDirectory }
            val files = filtered.filter { !it.isDirectory }

            val sortedFolders = when (criteria.sortOption) {
                SortOption.NAME_ASC -> folders.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> folders.sortedByDescending { it.name.lowercase() }
                SortOption.DATE_DESC -> folders.sortedByDescending { it.modifiedAt }
                SortOption.DATE_ASC -> folders.sortedBy { it.modifiedAt }
                SortOption.SIZE_DESC -> folders.sortedByDescending { it.sizeBytes }
                SortOption.SIZE_ASC -> folders.sortedBy { it.sizeBytes }
            }

            val sortedFiles = when (criteria.sortOption) {
                SortOption.NAME_ASC -> files.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
                SortOption.DATE_DESC -> files.sortedByDescending { it.modifiedAt }
                SortOption.DATE_ASC -> files.sortedBy { it.modifiedAt }
                SortOption.SIZE_DESC -> files.sortedByDescending { it.sizeBytes }
                SortOption.SIZE_ASC -> files.sortedBy { it.sizeBytes }
            }

            sortedFolders + sortedFiles
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFolders: StateFlow<List<VaultEntry>> = encryptedIndex.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val thumbnailCache = object : LruCache<String, Bitmap>(96) {
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && oldValue != newValue && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    private val thumbnailSemaphore = kotlinx.coroutines.sync.Semaphore(2)

    init {
        viewModelScope.launch {
            vaultSession.isUnlocked.collect { unlocked ->
                if (!unlocked) {
                    thumbnailCache.evictAll()
                }
            }
        }
    }

    fun setParentId(parentId: String?) {
        _currentParentId.value = parentId
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setTypeFilter(filter: FileTypeFilter) {
        _typeFilter.value = filter
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun createFolder(name: String, parentId: String?) {
        if (name.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val folder = VaultEntry(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                parentId = parentId,
                isDirectory = true,
                mimeType = null,
                sizeBytes = 0L,
                blobName = null,
                fileSalt = null,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
            encryptedIndex.addEntry(folder)
            try {
                vaultSession.withMasterKey { masterKey ->
                    encryptedIndex.saveIndex(masterKey)
                }
            } catch (e: Exception) {
                Log.e("VaultaBrowser", "Failed to save index after createFolder", e)
            }
        }
    }

    fun autoOrganize(targetParentId: String?) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch(ioDispatcher) {
            try {
                val currentEntries = encryptedIndex.getEntriesByParent(targetParentId).first()
                val filesToOrganize = currentEntries.filter { !it.isDirectory }
                if (filesToOrganize.isEmpty()) {
                    _userMessage.value = "No loose files found to organize in this folder"
                    return@launch
                }

                val existingFolders = currentEntries.filter { it.isDirectory }
                    .associateBy { it.name.lowercase() }
                    .toMutableMap()

                suspend fun getOrCreateFolder(categoryName: String): VaultEntry {
                    val existing = existingFolders[categoryName.lowercase()]
                    if (existing != null) return existing

                    val newFolder = VaultEntry(
                        id = UUID.randomUUID().toString(),
                        name = categoryName,
                        parentId = targetParentId,
                        isDirectory = true,
                        mimeType = null,
                        sizeBytes = 0L,
                        blobName = null,
                        fileSalt = null,
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis(),
                        syncStatus = SyncStatus.PENDING
                    )
                    encryptedIndex.addEntry(newFolder)
                    existingFolders[categoryName.lowercase()] = newFolder
                    return newFolder
                }

                var movedCount = 0
                for (file in filesToOrganize) {
                    val category = categorizeEntry(file) ?: continue
                    val targetFolder = getOrCreateFolder(category)
                    if (file.parentId == targetFolder.id) continue

                    val updatedFile = file.copy(
                        parentId = targetFolder.id,
                        modifiedAt = System.currentTimeMillis()
                    )
                    encryptedIndex.updateEntry(updatedFile)
                    movedCount++
                }

                if (movedCount > 0) {
                    vaultSession.withMasterKey { masterKey ->
                        encryptedIndex.saveIndex(masterKey)
                    }
                    _userMessage.value = "Organized $movedCount files into category folders"
                } else {
                    _userMessage.value = "No files matched organization categories"
                }
            } catch (e: Exception) {
                Log.e("VaultaBrowser", "Failed to auto-organize files", e)
                _error.value = "Failed to organize: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importFile(uri: Uri, parentId: String?) {
        importFiles(listOf(uri), parentId)
    }

    fun importFiles(uris: List<Uri>, parentId: String?) {
        if (uris.isEmpty()) return
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch(ioDispatcher) {
            try {
                for ((index, uri) in uris.withIndex()) {
                    val fileName = safAccessor.getFileName(uri) ?: "imported_file_${System.currentTimeMillis()}"
                    val rawMime = safAccessor.getMimeType(uri)
                    val ext = fileName.substringAfterLast('.', "").lowercase()
                    val mappedMime = if (ext.isNotEmpty()) android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) else null
                    val mimeType = when {
                        !rawMime.isNullOrBlank() && rawMime != "application/octet-stream" -> rawMime
                        mappedMime != null -> mappedMime
                        else -> rawMime ?: "application/octet-stream"
                    }
                    val fileSize = safAccessor.getFileSize(uri)

                    _importProgress.value = ImportProgress(
                        fileName = fileName,
                        currentFileIndex = index + 1,
                        totalFiles = uris.size,
                        bytesProcessed = 0L,
                        totalBytes = fileSize,
                        percentage = 0
                    )

                    Log.d("VaultaBrowser", "📥 [${index + 1}/${uris.size}] Starting import: '$fileName', size: $fileSize bytes, mime: $mimeType")

                    val blobName = UUID.randomUUID().toString() + ".blob"
                    val fileSalt = keyDerivation.generateSalt()

                    vaultSession.withMasterKey { masterKey ->
                        val fileKey = keyDerivation.deriveFileKey(masterKey, fileSalt)
                        try {
                            val source = safAccessor.readSource(uri)
                            blobStore.writeBlob(blobName, source, fileKey, fileSize) { bytesWritten, total ->
                                val pct = if (total > 0) ((bytesWritten * 100) / total).toInt().coerceIn(0, 100) else 0
                                _importProgress.value = ImportProgress(
                                    fileName = fileName,
                                    currentFileIndex = index + 1,
                                    totalFiles = uris.size,
                                    bytesProcessed = bytesWritten,
                                    totalBytes = total,
                                    percentage = pct
                                )
                            }
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }

                        val entry = VaultEntry(
                            id = UUID.randomUUID().toString(),
                            name = fileName,
                            parentId = parentId,
                            isDirectory = false,
                            mimeType = mimeType,
                            sizeBytes = fileSize,
                            blobName = blobName,
                            fileSalt = fileSalt,
                            createdAt = System.currentTimeMillis(),
                            modifiedAt = System.currentTimeMillis(),
                            syncStatus = SyncStatus.PENDING
                        )
                        encryptedIndex.addEntry(entry)
                        Log.d("VaultaBrowser", "✅ File '$fileName' ($fileSize bytes) encrypted and added to vault successfully!")
                    }
                }
                vaultSession.withMasterKey { masterKey ->
                    encryptedIndex.saveIndex(masterKey)
                }
            } catch (e: Exception) {
                Log.e("VaultaBrowser", "❌ Failed to encrypt and import files", e)
                _error.value = "Failed to encrypt and import: ${e.message}"
            } finally {
                _importProgress.value = null
                _isLoading.value = false
            }
        }
    }

    fun renameEntry(id: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            try {
                val entry = encryptedIndex.getEntry(id) ?: return@launch
                val updated = entry.copy(
                    name = newName.trim(),
                    modifiedAt = System.currentTimeMillis()
                )
                encryptedIndex.updateEntry(updated)
                vaultSession.withMasterKey { masterKey ->
                    encryptedIndex.saveIndex(masterKey)
                }
            } catch (e: Exception) {
                _error.value = "Failed to rename: ${e.message}"
            }
        }
    }

    fun moveEntry(id: String, targetParentId: String?) {
        if (id == targetParentId) return
        viewModelScope.launch(ioDispatcher) {
            try {
                val entry = encryptedIndex.getEntry(id) ?: return@launch
                val updated = entry.copy(
                    parentId = targetParentId,
                    modifiedAt = System.currentTimeMillis()
                )
                encryptedIndex.updateEntry(updated)
                vaultSession.withMasterKey { masterKey ->
                    encryptedIndex.saveIndex(masterKey)
                }
            } catch (e: Exception) {
                _error.value = "Failed to move: ${e.message}"
            }
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        viewModelScope.launch(ioDispatcher) {
            try {
                deleteEntryRecursive(entry)

                vaultSession.withMasterKey { masterKey ->
                    encryptedIndex.saveIndex(masterKey)
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete: ${e.message}"
            }
        }
    }

    private suspend fun deleteEntryRecursive(entry: VaultEntry) {
        if (entry.isDirectory) {
            val children = encryptedIndex.getEntriesByParent(entry.id).first()
            for (child in children) {
                deleteEntryRecursive(child)
            }
        } else {
            entry.blobName?.let { blobName ->
                blobStore.deleteBlob(blobName)
            }
        }
        encryptedIndex.removeEntry(entry.id)
        thumbnailCache.remove(entry.id)
    }

    fun exportFile(entry: VaultEntry, destinationUri: Uri) {
        val blobName = entry.blobName
        val fileSalt = entry.fileSalt
        if (entry.isDirectory || blobName == null || fileSalt == null) return
        _isLoading.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                val fileKey = vaultSession.withMasterKey { masterKey ->
                    keyDerivation.deriveFileKey(masterKey, fileSalt)
                }

                try {
                    val sink = safAccessor.writeSink(destinationUri)
                    blobStore.decryptBlobToSink(blobName, sink, fileKey)
                } finally {
                    SecureMemory.wipe(fileKey)
                }
            } catch (e: Exception) {
                _error.value = "Failed to decrypt and export file: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun loadThumbnail(entry: VaultEntry): Bitmap? = withContext(ioDispatcher) {
        val blobName = entry.blobName ?: return@withContext null
        val fileSalt = entry.fileSalt ?: return@withContext null
        if (entry.isDirectory) return@withContext null

        val cached = thumbnailCache.get(entry.id)
        if (cached != null && !cached.isRecycled) return@withContext cached

        val ext = entry.name.substringAfterLast('.', "").lowercase()
        val isImg = isImage(entry.mimeType.orEmpty().lowercase(), ext)
        if (!isImg) return@withContext null

        // Avoid decoding giant files for thumbnail preview to prevent heap memory exhaustion
        if (entry.sizeBytes > 25 * 1024 * 1024L) return@withContext null

        thumbnailSemaphore.withPermit {
            val recheck = thumbnailCache.get(entry.id)
            if (recheck != null && !recheck.isRecycled) return@withPermit recheck

            try {
                val fileKey = vaultSession.withMasterKey { masterKey ->
                    keyDerivation.deriveFileKey(masterKey, fileSalt)
                }

                val decryptedBytes = try {
                    blobStore.readBlobBytes(blobName, fileKey)
                } finally {
                    SecureMemory.wipe(fileKey)
                }

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, options)

                var sampleSize = 1
                val targetSize = 200
                if (options.outHeight > targetSize || options.outWidth > targetSize) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / sampleSize) >= targetSize && (halfWidth / sampleSize) >= targetSize) {
                        sampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size, decodeOptions)
                if (bitmap != null) {
                    thumbnailCache.put(entry.id, bitmap)
                }
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    fun lockVault() {
        viewModelScope.launch(ioDispatcher) {
            try {
                encryptedIndex.clear()
                thumbnailCache.evictAll()
            } catch (_: Exception) {}
        }
        vaultSession.lock()
    }

    companion object {
        val IMAGE_EXTS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif",
            "svg", "ico", "tiff", "tif", "raw", "cr2", "nef", "arw", "dng", "psd"
        )
        val VIDEO_EXTS = setOf(
            "mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "wmv", "m4v", "ts", "mpg", "mpeg"
        )
        val AUDIO_EXTS = setOf(
            "mp3", "wav", "m4a", "aac", "flac", "ogg", "opus", "wma", "amr", "mid", "midi", "aiff"
        )
        val DOC_EXTS = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "csv", "tsv", "ppt", "pptx",
            "odt", "ods", "odp", "rtf", "epub", "pages", "numbers", "key"
        )
        val TEXT_EXTS = setOf(
            "txt", "text", "log", "md", "markdown", "rst", "tex"
        )
        val CODE_EXTS = setOf(
            "json", "xml", "yaml", "yml", "conf", "ini", "properties", "sql", "sh", "bat", "ps1",
            "py", "kt", "kts", "java", "js", "jsx", "ts", "tsx", "html", "htm", "css", "scss",
            "c", "cpp", "h", "hpp", "rs", "go", "php", "rb", "swift", "dart", "gradle"
        )
        val ARCHIVE_EXTS = setOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz", "iso", "apk", "dmg", "jar"
        )

        fun isImage(mime: String, ext: String): Boolean =
            mime.startsWith("image/") || ext in IMAGE_EXTS

        fun isVideo(mime: String, ext: String): Boolean =
            mime.startsWith("video/") || ext in VIDEO_EXTS

        fun isAudio(mime: String, ext: String): Boolean =
            mime.startsWith("audio/") || ext in AUDIO_EXTS

        fun isDocument(mime: String, ext: String): Boolean =
            ext in DOC_EXTS || mime in setOf("application/pdf", "application/msword", "text/csv", "text/tab-separated-values") ||
            mime.startsWith("application/vnd.ms-") ||
            mime.contains("opendocument") || mime.contains("openxmlformats") ||
            mime.contains("spreadsheet") || mime.contains("csv")

        fun isText(mime: String, ext: String): Boolean =
            (mime.startsWith("text/") && ext !in CODE_EXTS && ext !in DOC_EXTS) || ext in TEXT_EXTS

        fun isCode(mime: String, ext: String): Boolean =
            ext in CODE_EXTS || (mime in setOf("application/json", "application/xml", "application/x-yaml", "application/javascript", "application/sql") && ext !in DOC_EXTS)

        fun isArchive(mime: String, ext: String): Boolean =
            ext in ARCHIVE_EXTS || mime in setOf("application/zip", "application/x-rar-compressed", "application/x-7z-compressed", "application/x-tar", "application/gzip")

        fun matchesTypeFilter(entry: VaultEntry, filter: FileTypeFilter): Boolean {
            if (entry.isDirectory) return false
            val mime = entry.mimeType.orEmpty().lowercase()
            val ext = entry.name.substringAfterLast('.', "").lowercase()
            return when (filter) {
                FileTypeFilter.ALL -> true
                FileTypeFilter.IMAGES -> isImage(mime, ext)
                FileTypeFilter.VIDEOS -> isVideo(mime, ext)
                FileTypeFilter.AUDIO -> isAudio(mime, ext)
                FileTypeFilter.DOCUMENTS -> isDocument(mime, ext)
                FileTypeFilter.TEXT -> isText(mime, ext)
                FileTypeFilter.CODE -> isCode(mime, ext)
                FileTypeFilter.ARCHIVES -> isArchive(mime, ext)
            }
        }

        fun categorizeEntry(entry: VaultEntry): String? {
            if (entry.isDirectory) return null
            val mime = entry.mimeType.orEmpty().lowercase()
            val ext = entry.name.substringAfterLast('.', "").lowercase()
            return when {
                isImage(mime, ext) -> "Images"
                isVideo(mime, ext) -> "Videos"
                isAudio(mime, ext) -> "Audio"
                isDocument(mime, ext) -> "Documents"
                isText(mime, ext) -> "Text Documents"
                isCode(mime, ext) -> "Code & Scripts"
                isArchive(mime, ext) -> "Archives"
                else -> null
            }
        }
    }
}
