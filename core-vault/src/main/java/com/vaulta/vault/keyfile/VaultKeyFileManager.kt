package com.vaulta.vault.keyfile

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages reading and writing vault.key files through Android's
 * Storage Access Framework (SAF).
 */
interface VaultKeyFileManager {

    /**
     * Reads and deserializes a [VaultKeyFile] from the given SAF [Uri].
     *
     * @param uri Content URI pointing to the vault.key file.
     * @return The deserialized [VaultKeyFile].
     * @throws InvalidVaultKeyFileException if the file content is invalid.
     * @throws java.io.IOException if reading fails.
     */
    fun readKeyFile(uri: Uri): VaultKeyFile

    /**
     * Serializes and writes a [VaultKeyFile] to the given SAF [Uri].
     *
     * @param uri Content URI pointing to the vault.key file location.
     * @param keyFile The [VaultKeyFile] to write.
     * @throws java.io.IOException if writing fails.
     */
    fun writeKeyFile(uri: Uri, keyFile: VaultKeyFile)
}

/**
 * Default implementation of [VaultKeyFileManager] using [android.content.ContentResolver].
 */
@Singleton
class VaultKeyFileManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serializer: VaultKeyFileSerializer,
) : VaultKeyFileManager {

    override fun readKeyFile(uri: Uri): VaultKeyFile {
        val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw InvalidVaultKeyFileException("Could not open vault.key file at: $uri")

        return serializer.deserialize(bytes)
    }

    override fun writeKeyFile(uri: Uri, keyFile: VaultKeyFile) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {}

        val bytes = serializer.serialize(keyFile)

        val outputStream = try {
            context.contentResolver.openOutputStream(uri, "wt")
        } catch (_: Exception) {
            context.contentResolver.openOutputStream(uri, "w") ?: context.contentResolver.openOutputStream(uri)
        } ?: throw IllegalStateException("Could not open output stream for vault.key at: $uri")

        outputStream.use { os ->
            os.write(bytes)
            os.flush()
        }
    }
}
