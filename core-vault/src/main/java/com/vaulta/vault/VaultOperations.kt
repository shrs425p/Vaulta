package com.vaulta.vault

import android.net.Uri
import com.vaulta.crypto.Argon2Params
import com.vaulta.crypto.KeyDerivation
import com.vaulta.crypto.KeyUnwrapException
import com.vaulta.crypto.KeyWrapping
import com.vaulta.crypto.KeyDerivationImpl
import com.vaulta.crypto.SecureMemory
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.vault.keyfile.VaultKeyFile
import com.vaulta.vault.keyfile.VaultKeyFileManager
import com.vaulta.vault.recovery.RecoveryCodeGenerator
import com.vaulta.vault.session.VaultSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class RecoveryResult(
    val recoveryCode: String
)

interface VaultOperations {
    suspend fun createVault(password: CharArray, keyFileUri: Uri): RecoveryResult
    suspend fun unlockWithPassword(password: CharArray, keyFileUri: Uri)
    suspend fun changePassword(currentPassword: CharArray, newPassword: CharArray, keyFileUri: Uri)
    suspend fun recoverWithCode(recoveryCode: String, keyFileUri: Uri, newPassword: CharArray)
}

@Singleton
class VaultOperationsImpl @Inject constructor(
    private val keyDerivation: KeyDerivation,
    private val keyWrapping: KeyWrapping,
    private val vaultKeyFileManager: VaultKeyFileManager,
    private val vaultSession: VaultSession,
    private val recoveryCodeGenerator: RecoveryCodeGenerator,
    private val encryptedIndex: EncryptedIndex
) : VaultOperations {

    private val mutex = Mutex()

    override suspend fun createVault(password: CharArray, keyFileUri: Uri): RecoveryResult = mutex.withLock {
        val masterKey = keyDerivation.generateMasterKey()
        val salt = keyDerivation.generateSalt()
        val recoveryCode = recoveryCodeGenerator.generate()
        val recoveryChars = recoveryCodeGenerator.toCharArray(recoveryCode)

        try {
            val params = Argon2Params()
            val passwordWrappingKey = keyDerivation.deriveKey(password, salt, params)
            val slotA = try {
                keyWrapping.wrap(masterKey, passwordWrappingKey)
            } finally {
                SecureMemory.wipe(passwordWrappingKey)
            }

            val recoveryWrappingKey = keyDerivation.deriveKey(recoveryChars, salt, params)
            val slotB = try {
                keyWrapping.wrap(masterKey, recoveryWrappingKey)
            } finally {
                SecureMemory.wipe(recoveryWrappingKey)
            }

            val keyFile = VaultKeyFile(
                argon2Params = params,
                salt = salt,
                wrappedMasterKeySlotA = slotA,
                wrappedMasterKeySlotB = slotB
            )
            vaultKeyFileManager.writeKeyFile(keyFileUri, keyFile)
            encryptedIndex.clear()
            encryptedIndex.saveIndex(masterKey)
            vaultSession.unlock(masterKey)

            return RecoveryResult(recoveryCode = recoveryCode)
        } finally {
            SecureMemory.wipe(masterKey)
            SecureMemory.wipe(password)
            SecureMemory.wipe(recoveryChars)
        }
    }

    override suspend fun unlockWithPassword(password: CharArray, keyFileUri: Uri) = mutex.withLock {
        val keyFile = vaultKeyFileManager.readKeyFile(keyFileUri)
        val wrappingKey = keyDerivation.deriveKey(password, keyFile.salt, keyFile.argon2Params)
        try {
            val masterKey = unwrapSlotWithFallback(
                slotData = keyFile.wrappedMasterKeySlotA,
                primaryWrappingKey = wrappingKey,
                passwordOrCode = password,
                salt = keyFile.salt,
                params = keyFile.argon2Params
            )
            try {
                encryptedIndex.loadIndex(masterKey)
                vaultSession.unlock(masterKey)
            } finally {
                SecureMemory.wipe(masterKey)
            }
        } finally {
            SecureMemory.wipe(wrappingKey)
            SecureMemory.wipe(password)
        }
    }

    override suspend fun changePassword(
        currentPassword: CharArray,
        newPassword: CharArray,
        keyFileUri: Uri
    ) = mutex.withLock {
        val keyFile = vaultKeyFileManager.readKeyFile(keyFileUri)
        val currentWrappingKey = keyDerivation.deriveKey(currentPassword, keyFile.salt, keyFile.argon2Params)
        val masterKey = try {
            unwrapSlotWithFallback(
                slotData = keyFile.wrappedMasterKeySlotA,
                primaryWrappingKey = currentWrappingKey,
                passwordOrCode = currentPassword,
                salt = keyFile.salt,
                params = keyFile.argon2Params
            )
        } finally {
            SecureMemory.wipe(currentWrappingKey)
            SecureMemory.wipe(currentPassword)
        }

        try {
            val newWrappingKey = keyDerivation.deriveKey(newPassword, keyFile.salt, keyFile.argon2Params)
            val newSlotA = try {
                keyWrapping.wrap(masterKey, newWrappingKey)
            } finally {
                SecureMemory.wipe(newWrappingKey)
                SecureMemory.wipe(newPassword)
            }

            val updatedKeyFile = keyFile.copy(wrappedMasterKeySlotA = newSlotA)
            vaultKeyFileManager.writeKeyFile(keyFileUri, updatedKeyFile)
            encryptedIndex.loadIndex(masterKey)
            vaultSession.unlock(masterKey)
        } finally {
            SecureMemory.wipe(masterKey)
        }
    }

    override suspend fun recoverWithCode(
        recoveryCode: String,
        keyFileUri: Uri,
        newPassword: CharArray
    ) = mutex.withLock {
        val keyFile = vaultKeyFileManager.readKeyFile(keyFileUri)
        val recoveryChars = recoveryCodeGenerator.toCharArray(recoveryCode)
        val recoveryWrappingKey = try {
            keyDerivation.deriveKey(recoveryChars, keyFile.salt, keyFile.argon2Params)
        } finally {
            SecureMemory.wipe(recoveryChars)
        }

        val masterKey = try {
            val recChars = recoveryCodeGenerator.toCharArray(recoveryCode)
            try {
                unwrapSlotWithFallback(
                    slotData = keyFile.wrappedMasterKeySlotB,
                    primaryWrappingKey = recoveryWrappingKey,
                    passwordOrCode = recChars,
                    salt = keyFile.salt,
                    params = keyFile.argon2Params
                )
            } finally {
                SecureMemory.wipe(recChars)
            }
        } finally {
            SecureMemory.wipe(recoveryWrappingKey)
        }

        try {
            val newWrappingKey = keyDerivation.deriveKey(newPassword, keyFile.salt, keyFile.argon2Params)
            val newSlotA = try {
                keyWrapping.wrap(masterKey, newWrappingKey)
            } finally {
                SecureMemory.wipe(newWrappingKey)
                SecureMemory.wipe(newPassword)
            }

            val updatedKeyFile = keyFile.copy(wrappedMasterKeySlotA = newSlotA)
            vaultKeyFileManager.writeKeyFile(keyFileUri, updatedKeyFile)
            encryptedIndex.loadIndex(masterKey)
            vaultSession.unlock(masterKey)
        } finally {
            SecureMemory.wipe(masterKey)
        }
    }

    private fun unwrapSlotWithFallback(
        slotData: ByteArray,
        primaryWrappingKey: ByteArray,
        passwordOrCode: CharArray,
        salt: ByteArray,
        params: Argon2Params
    ): ByteArray {
        return try {
            keyWrapping.unwrap(slotData, primaryWrappingKey)
        } catch (e: KeyUnwrapException) {
            // Only attempt PBKDF2 fallback if keyfile parameters explicitly indicate legacy non-Argon2 parameters
            if (params.memoryCostKb <= 0) {
                val pbkdf2Key = (keyDerivation as? KeyDerivationImpl)?.deriveKeyPbkdf2(
                    password = passwordOrCode,
                    salt = salt,
                    iterations = (params.iterations * 25000).coerceAtLeast(100000)
                ) ?: throw e
                try {
                    keyWrapping.unwrap(slotData, pbkdf2Key)
                } finally {
                    SecureMemory.wipe(pbkdf2Key)
                }
            } else {
                throw e
            }
        }
    }
}
