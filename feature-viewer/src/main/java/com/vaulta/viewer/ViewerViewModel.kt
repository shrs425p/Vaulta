package com.vaulta.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaulta.crypto.KeyDerivation
import com.vaulta.crypto.SecureMemory
import com.vaulta.storage.blob.BlobStore
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.storage.index.VaultEntry
import com.vaulta.vault.session.VaultSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface ViewerUiState {
    data object Loading : ViewerUiState
    
    data class ImageReady(
        val entry: VaultEntry,
        val bitmap: Bitmap,
        val data: ByteArray? = null,
        val imageFile: File? = null
    ) : ViewerUiState
    
    data class VideoReady(
        val entry: VaultEntry,
        val videoFile: File
    ) : ViewerUiState
    
    data class AudioReady(
        val entry: VaultEntry,
        val audioFile: File
    ) : ViewerUiState
    
    data class PdfReady(
        val entry: VaultEntry,
        val pdfFile: File,
        val pageCount: Int
    ) : ViewerUiState

    data class CsvReady(
        val entry: VaultEntry,
        val rows: List<List<String>>,
        val rawText: String,
        val data: ByteArray
    ) : ViewerUiState
    
    data class TextReady(
        val entry: VaultEntry,
        val textContent: String,
        val data: ByteArray
    ) : ViewerUiState
    
    data class DocumentReady(
        val entry: VaultEntry,
        val docType: String,
        val data: ByteArray,
        val previewText: String? = null,
        val slides: List<PptxSlide> = emptyList(),
        val docxDoc: DocxDocument? = null
    ) : ViewerUiState
    
    data class BinaryReady(
        val entry: VaultEntry,
        val file: File
    ) : ViewerUiState
    
    data class Error(val message: String) : ViewerUiState
}

@HiltViewModel
class ViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptedIndex: EncryptedIndex,
    private val blobStore: BlobStore,
    private val keyDerivation: KeyDerivation,
    private val syncProvider: com.vaulta.vault.sync.SyncProvider,
    private val vaultSession: VaultSession
) : ViewModel() {

    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private var currentLoadedEntryId: String? = null
    private var decryptedBytes: ByteArray? = null
    private var currentLoadedBitmap: Bitmap? = null
    private val tempFiles = mutableListOf<File>()
    private var pdfRenderer: PdfRenderer? = null
    private var pdfFileDescriptor: ParcelFileDescriptor? = null

    private val pdfPageCache = object : android.util.LruCache<Int, Bitmap>(16) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && oldValue != newValue && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    init {
        viewModelScope.launch {
            vaultSession.isUnlocked.collect { unlocked ->
                if (!unlocked) {
                    cleanupOldResources()
                    _uiState.value = ViewerUiState.Error("Vault locked")
                }
            }
        }
    }

    fun loadFile(entryId: String, forceReload: Boolean = false) {
        if (!forceReload && currentLoadedEntryId == entryId && _uiState.value !is ViewerUiState.Error && _uiState.value !is ViewerUiState.Loading) {
            Log.d("VaultaViewer", "File '$entryId' is already loaded and ready in memory/disk. Skipping redundant decryption.")
            return
        }

        currentLoadedEntryId = entryId
        _uiState.value = ViewerUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cleanupOldResources()

                val entry = encryptedIndex.getEntry(entryId)
                    ?: throw IllegalArgumentException("File entry not found in index")

                if (!entry.isAvailableLocally) {
                    Log.d("VaultaViewer", "File '${entry.name}' is cloud-only. Downloading blob on-demand...")
                    val downloadRes = syncProvider.downloadSpecificBlob(entry.id)
                    if (downloadRes.isFailure) {
                        val msg = downloadRes.exceptionOrNull()?.message ?: "Failed downloading cloud blob"
                        _uiState.value = ViewerUiState.Error("Cloud Download Error: $msg")
                        return@launch
                    }
                }

                val blobName = entry.blobName
                    ?: throw IllegalArgumentException("File has no associated blob")
                val salt = entry.fileSalt
                    ?: throw IllegalArgumentException("File has no encryption salt")

                val fileKey = vaultSession.withMasterKey { masterKey ->
                    keyDerivation.deriveFileKey(masterKey, salt)
                }

                val name = entry.name.lowercase()
                val ext = name.substringAfterLast('.', "")
                val mime = (entry.mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
                    ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)).orEmpty().lowercase()

                Log.d("VaultaViewer", "Loading file: '${entry.name}', size: ${entry.sizeBytes} bytes, ext: '$ext', mime: '$mime'")

                val isMediaStreaming = mime.startsWith("video/") || 
                    mime.startsWith("audio/") || 
                    mime == "application/pdf" || 
                    ext in setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v", "ts", "mp3", "wav", "m4a", "aac", "flac", "ogg", "opus", "wma", "mid", "pdf")

                val isImage = mime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "ico")

                if (!isMediaStreaming) {
                    if (isImage && entry.sizeBytes > MAX_IMAGE_BYTES) {
                        SecureMemory.wipe(fileKey)
                        _uiState.value = ViewerUiState.Error("Image is too large (${entry.sizeBytes / (1024 * 1024)} MB) to decode safely in memory. Please export to view.")
                        return@launch
                    } else if (!isImage && entry.sizeBytes > MAX_IN_MEMORY_PREVIEW_BYTES) {
                        SecureMemory.wipe(fileKey)
                        _uiState.value = ViewerUiState.Error("File is too large (${entry.sizeBytes / (1024 * 1024)} MB) to preview safely in memory. Please export to view.")
                        return@launch
                    }
                }

                when {
                    // Videos -> Stream decrypt directly to disk (ZERO heap overhead, prevents OOM)
                    mime.startsWith("video/") || ext in setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v", "ts") -> {
                        val videoFile = createSecureTempFile("vid_${entry.id}_${entry.name}")
                        try {
                            blobStore.decryptBlobToFile(blobName, videoFile, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        Log.d("VaultaViewer", "Video streamed to temp file: ${videoFile.length()} bytes")
                        _uiState.value = ViewerUiState.VideoReady(entry, videoFile)
                    }

                    // Audio -> Stream decrypt directly to disk
                    mime.startsWith("audio/") || ext in setOf("mp3", "wav", "m4a", "aac", "flac", "ogg", "opus", "wma", "mid") -> {
                        val audioFile = createSecureTempFile("aud_${entry.id}_${entry.name}")
                        try {
                            blobStore.decryptBlobToFile(blobName, audioFile, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        Log.d("VaultaViewer", "Audio streamed to temp file: ${audioFile.length()} bytes")
                        _uiState.value = ViewerUiState.AudioReady(entry, audioFile)
                    }

                    // PDF Documents -> Stream decrypt directly to disk
                    mime == "application/pdf" || ext == "pdf" -> {
                        val pdfFile = createSecureTempFile("pdf_${entry.id}.pdf")
                        try {
                            blobStore.decryptBlobToFile(blobName, pdfFile, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        pdfFileDescriptor = pfd
                        val renderer = PdfRenderer(pfd)
                        pdfRenderer = renderer
                        Log.d("VaultaViewer", "PDF streamed to temp file: ${renderer.pageCount} pages")
                        _uiState.value = ViewerUiState.PdfReady(entry, pdfFile, renderer.pageCount)
                    }

                    // Images (photos, screenshots, logos) -> Stream decrypt directly to RAM (never touches flash storage)
                    mime.startsWith("image/") || ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "ico") -> {
                        val bytes = try {
                            blobStore.readBlobBytes(blobName, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        decryptedBytes = bytes
                        val bitmap = decodeSampledBitmap(bytes)
                        if (bitmap != null) {
                            currentLoadedBitmap = bitmap
                            _uiState.value = ViewerUiState.ImageReady(entry, bitmap, bytes, null)
                        } else {
                            _uiState.value = ViewerUiState.Error("Failed to decode image data")
                        }
                    }

                    // CSV / TSV tabular data
                    ext == "csv" || ext == "tsv" || mime == "text/csv" || mime == "text/comma-separated-values" -> {
                        val bytes = try {
                            blobStore.readBlobBytes(blobName, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        decryptedBytes = bytes
                        val text = String(bytes, Charsets.UTF_8)
                        val delimiter = if (ext == "tsv") '\t' else ','
                        val rows = DocumentPreviewHelper.parseCsv(text, delimiter)
                        _uiState.value = ViewerUiState.CsvReady(entry, rows, text, bytes)
                    }

                    // Microsoft Word & OpenDocument Text
                    ext in setOf("doc", "docx", "dot", "dotx", "rtf", "odt", "pages") -> {
                        val bytes = try {
                            blobStore.readBlobBytes(blobName, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        decryptedBytes = bytes
                        val docxDoc = DocumentPreviewHelper.extractDocxDocument(bytes)
                        val previewText = docxDoc.rawText.ifBlank {
                            when (ext) {
                                "docx" -> DocumentPreviewHelper.extractDocxText(bytes)
                                "doc" -> DocumentPreviewHelper.extractDocxText(bytes) ?: DocumentPreviewHelper.extractBinaryOfficeText(bytes)
                                "odt" -> DocumentPreviewHelper.extractOdtText(bytes)
                                "rtf" -> DocumentPreviewHelper.extractRtfText(String(bytes, Charsets.UTF_8))
                                else -> DocumentPreviewHelper.extractDocxText(bytes) ?: DocumentPreviewHelper.extractBinaryOfficeText(bytes)
                            }
                        }
                        val docType = when {
                            ext.contains("doc") -> "Microsoft Word Document"
                            ext == "odt" -> "OpenDocument Text"
                            ext == "rtf" -> "Rich Text Document"
                            else -> "Document ($ext)"
                        }
                        _uiState.value = ViewerUiState.DocumentReady(entry, docType, bytes, previewText, emptyList(), docxDoc)
                    }

                    // Microsoft PowerPoint & OpenDocument Presentation
                    ext in setOf("ppt", "pptx", "pot", "potx", "odp", "key") -> {
                        val bytes = try {
                            blobStore.readBlobBytes(blobName, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        decryptedBytes = bytes
                        val slides = DocumentPreviewHelper.extractPptxSlides(bytes)
                        val previewText = if (slides.isNotEmpty()) {
                            slides.joinToString("\n\n") { "Slide ${it.slideNumber}: ${it.rawText}" }
                        } else {
                            DocumentPreviewHelper.extractBinaryOfficeText(bytes)
                        }
                        val docType = if (ext.contains("ppt")) "PowerPoint Presentation" else "Presentation ($ext)"
                        _uiState.value = ViewerUiState.DocumentReady(entry, docType, bytes, previewText, slides)
                    }

                    // Microsoft Excel & OpenDocument Spreadsheet
                    ext in setOf("xls", "xlsx", "xlt", "xltx", "ods", "numbers") -> {
                        val bytes = try {
                            blobStore.readBlobBytes(blobName, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        decryptedBytes = bytes
                        val docType = if (ext.contains("xl")) "Microsoft Excel Spreadsheet" else "Spreadsheet ($ext)"
                        val xlsxRows = if (ext == "xlsx") DocumentPreviewHelper.extractXlsxRows(bytes) else null
                        if (xlsxRows != null && xlsxRows.isNotEmpty()) {
                            val rawText = xlsxRows.joinToString("\n") { it.joinToString(", ") }
                            _uiState.value = ViewerUiState.CsvReady(entry, xlsxRows, rawText, bytes)
                        } else {
                            _uiState.value = ViewerUiState.DocumentReady(entry, docType, bytes)
                        }
                    }

                    // Plain Text, Code, Markup, Logs, Configurations
                    mime.startsWith("text/") || ext in setOf(
                        "txt", "md", "markdown", "json", "xml", "html", "htm", "css", "js", "ts",
                        "kt", "java", "py", "c", "cpp", "h", "yaml", "yml", "log", "sh", "properties",
                        "sql", "ini", "conf", "env", "gradle", "bat", "cmd", "tex"
                    ) -> {
                        val bytes = try {
                            blobStore.readBlobBytes(blobName, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        decryptedBytes = bytes
                        val text = String(bytes, Charsets.UTF_8)
                        _uiState.value = ViewerUiState.TextReady(entry, text, bytes)
                    }

                    // Other / Binary files
                    else -> {
                        val tempFile = createSecureTempFile("bin_${entry.id}_${entry.name}")
                        try {
                            blobStore.decryptBlobToFile(blobName, tempFile, fileKey)
                        } finally {
                            SecureMemory.wipe(fileKey)
                        }
                        _uiState.value = ViewerUiState.BinaryReady(entry, tempFile)
                    }
                }
            } catch (e: Exception) {
                Log.e("VaultaViewer", "Failed to load/decrypt file $entryId", e)
                _uiState.value = ViewerUiState.Error("Decryption error: ${e.message}")
            }
        }
    }

    fun renderPdfPage(pageIndex: Int): Bitmap? {
        val cached = pdfPageCache.get(pageIndex)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        val renderer = pdfRenderer ?: return null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
        return try {
            synchronized(renderer) {
                val page = renderer.openPage(pageIndex)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pdfPageCache.put(pageIndex, bitmap)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeSampledBitmapFromFile(file: File): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var sampleSize = 1
        val maxDim = 3840
        if (options.outHeight > maxDim || options.outWidth > maxDim) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / sampleSize) >= maxDim && (halfWidth / sampleSize) >= maxDim) {
                sampleSize *= 2
            }
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }

    private fun decodeSampledBitmap(bytes: ByteArray): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        var sampleSize = 1
        val maxDim = 3840
        if (options.outHeight > maxDim || options.outWidth > maxDim) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / sampleSize) >= maxDim && (halfWidth / sampleSize) >= maxDim) {
                sampleSize *= 2
            }
        }
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    private fun createSecureTempFile(name: String): File {
        val cacheSubdir = File(context.cacheDir, "viewer_temp").apply { mkdirs() }
        val file = File(cacheSubdir, name)
        if (file.exists()) file.delete()
        file.deleteOnExit()
        tempFiles.add(file)
        return file
    }

    fun cleanup() {
        cleanupOldResources()
        _uiState.value = ViewerUiState.Loading
        currentLoadedEntryId = null
    }

    private fun cleanupOldResources() {
        currentLoadedBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
        currentLoadedBitmap = null

        (_uiState.value as? ViewerUiState.ImageReady)?.bitmap?.let { bmp ->
            if (!bmp.isRecycled) bmp.recycle()
        }

        try {
            pdfRenderer?.close()
            pdfFileDescriptor?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        pdfFileDescriptor = null

        try {
            pdfPageCache.evictAll()
        } catch (_: Exception) {}

        for (file in tempFiles) {
            try {
                if (file.exists()) {
                    var remaining = file.length()
                    if (remaining > 0) {
                        val zeroBuffer = ByteArray(64 * 1024)
                        java.io.RandomAccessFile(file, "rws").use { raf ->
                            raf.seek(0)
                            while (remaining > 0) {
                                val writeSize = minOf(remaining, zeroBuffer.size.toLong()).toInt()
                                raf.write(zeroBuffer, 0, writeSize)
                                remaining -= writeSize
                            }
                            raf.fd.sync()
                        }
                    }
                    file.delete()
                }
            } catch (_: Exception) {
                try { file.delete() } catch (_: Exception) {}
            }
        }
        tempFiles.clear()

        decryptedBytes?.let {
            SecureMemory.wipe(it)
            decryptedBytes = null
        }
        com.vaulta.storage.cleanup.ViewerCachePurge.purgeViewerTemp(context)
    }

    override fun onCleared() {
        super.onCleared()
        cleanupOldResources()
    }

    companion object {
        private const val MAX_IN_MEMORY_PREVIEW_BYTES = 10 * 1024 * 1024L // 10 MB
        private const val MAX_IMAGE_BYTES = 25 * 1024 * 1024L // 25 MB
    }
}
