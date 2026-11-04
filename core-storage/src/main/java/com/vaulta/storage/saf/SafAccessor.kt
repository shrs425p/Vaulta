package com.vaulta.storage.saf

import android.net.Uri
import okio.Sink
import okio.Source

interface SafAccessor {
    fun readBytes(uri: Uri): ByteArray
    fun writeBytes(uri: Uri, data: ByteArray)
    fun readSource(uri: Uri): Source
    fun writeSink(uri: Uri): Sink
    fun getFileName(uri: Uri): String?
    fun getMimeType(uri: Uri): String?
    fun getFileSize(uri: Uri): Long
    fun deleteOriginal(uri: Uri): Boolean
    fun createNoMediaFile(folderUri: Uri): Boolean
}
