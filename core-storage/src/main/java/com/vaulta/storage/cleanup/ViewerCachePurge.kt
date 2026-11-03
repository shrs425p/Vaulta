package com.vaulta.storage.cleanup

import android.content.Context
import android.util.Log
import java.io.File
import java.io.RandomAccessFile

object ViewerCachePurge {

    private const val TAG = "ViewerCachePurge"

    /**
     * Purges temporary viewer and shared cache directories by securely zeroing out
     * file headers/contents and deleting the files and directory trees.
     */
    fun purgeViewerTemp(context: Context) {
        val targetDirs = listOf(
            File(context.cacheDir, "viewer_temp"),
            File(context.cacheDir, "shared_temp")
        )

        for (dir in targetDirs) {
            if (dir.exists()) {
                cleanDirectorySecurely(dir)
            }
        }
    }

    private fun cleanDirectorySecurely(dir: File) {
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    cleanDirectorySecurely(file)
                } else if (file.isFile) {
                    wipeAndDeleteFile(file)
                }
            }
            dir.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning cache dir ${dir.name}: ${e.message}")
        }
    }

    private fun wipeAndDeleteFile(file: File) {
        try {
            var remaining = file.length()
            if (remaining > 0) {
                // Securely overwrite the ENTIRE file with zeroes in blocks before deletion
                val zeroBuf = ByteArray(64 * 1024)
                RandomAccessFile(file, "rws").use { raf ->
                    raf.seek(0)
                    while (remaining > 0) {
                        val toWrite = minOf(remaining, zeroBuf.size.toLong()).toInt()
                        raf.write(zeroBuf, 0, toWrite)
                        remaining -= toWrite
                    }
                    raf.fd.sync()
                }
            }
            file.delete()
        } catch (e: Exception) {
            try {
                file.delete()
            } catch (_: Exception) {}
        }
    }
}
