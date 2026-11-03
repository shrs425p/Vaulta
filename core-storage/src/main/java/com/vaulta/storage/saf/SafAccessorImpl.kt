package com.vaulta.storage.saf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import okio.Sink
import okio.Source
import okio.sink
import okio.source
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafAccessorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SafAccessor {

    override fun readBytes(uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw FileNotFoundException("Could not open input stream for: $uri")
    }

    private fun openTruncatingOutputStream(uri: Uri): OutputStream {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")
        } catch (e: Exception) {
            context.contentResolver.openOutputStream(uri, "w")
        } ?: throw IOException("Could not open output stream for: $uri")
    }

    override fun writeBytes(uri: Uri, data: ByteArray) {
        openTruncatingOutputStream(uri).use { outputStream ->
            outputStream.write(data)
            outputStream.flush()
        }
    }

    override fun readSource(uri: Uri): Source {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Could not open input stream for: $uri")
        return inputStream.source()
    }

    override fun writeSink(uri: Uri): Sink {
        val outputStream = openTruncatingOutputStream(uri)
        return outputStream.sink()
    }

    override fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    override fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    override fun getFileSize(uri: Uri): Long {
        var size: Long = 0L
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.SIZE)
                if (index != -1 && !it.isNull(index)) {
                    size = it.getLong(index)
                }
            }
        }
        return size
    }
}
