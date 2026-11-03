package com.vaulta.vault.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vaulta.crypto.SecureMemory
import com.vaulta.storage.index.EncryptedIndex
import com.vaulta.vault.session.VaultSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricUnlockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultSession: VaultSession,
    private val encryptedIndex: EncryptedIndex
) {

    private val keyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private val secretFile: File
        get() = File(File(context.filesDir, "vaulta").apply { if (!exists()) mkdirs() }, "biometric_secret.enc")

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricEnabled(): Boolean {
        return canAuthenticate() && secretFile.exists() && secretFile.length() > 16 && keyStore.containsAlias(KEY_ALIAS)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }

            // Attempt StrongBox Hardware Security Module (Titan M / Knox Vault) isolation if supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE)
            ) {
                try {
                    builder.setIsStrongBoxBacked(true)
                    keyGenerator.init(builder.build())
                    keyGenerator.generateKey()
                    return keyStore.getKey(KEY_ALIAS, null) as SecretKey
                } catch (e: Exception) {
                    Log.w("BiometricUnlockManager", "StrongBox hardware keygen failed, falling back to standard TEE KeyStore", e)
                    builder.setIsStrongBoxBacked(false)
                }
            }

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Prepares biometric enrollment: wraps the active masterKey with hardware-backed Keystore key.
     * Takes ownership of `masterKey` and guarantees secure wiping after encryption or on failure.
     */
    fun promptEnableBiometrics(
        activity: FragmentActivity,
        masterKey: ByteArray,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!canAuthenticate()) {
            SecureMemory.wipe(masterKey)
            onError("Biometrics not available on this device")
            return
        }

        // Remove any prior key to ensure fresh hardware key generation with strict enrollment invalidation
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (_: Exception) {}

        val masterKeyCopy = masterKey.copyOf()
        // Wipe caller's array copy immediately
        SecureMemory.wipe(masterKey)

        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val cryptoObject = BiometricPrompt.CryptoObject(cipher)
            val executor = ContextCompat.getMainExecutor(activity)

            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val authenticatedCipher = result.cryptoObject?.cipher
                        if (authenticatedCipher == null) {
                            SecureMemory.wipe(masterKeyCopy)
                            onError("Biometric cipher missing")
                            return
                        }

                        try {
                            val iv = authenticatedCipher.iv
                            val ciphertext = authenticatedCipher.doFinal(masterKeyCopy)
                            
                            // File format: 1 byte iv length + iv + ciphertext
                            secretFile.outputStream().use { os ->
                                os.write(iv.size)
                                os.write(iv)
                                os.write(ciphertext)
                            }
                            Log.i(TAG, "Biometric unlock successfully enrolled")
                            onSuccess()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed wrapping master key with biometrics", e)
                            onError("Failed saving biometric credentials: ${e.message}")
                        } finally {
                            SecureMemory.wipe(masterKeyCopy)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        SecureMemory.wipe(masterKeyCopy)
                        onError(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Enable Biometric Unlock")
                .setSubtitle("Confirm your fingerprint or biometric identity")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } catch (e: Exception) {
            SecureMemory.wipe(masterKeyCopy)
            Log.e(TAG, "Error starting biometric enrollment", e)
            onError(e.message ?: "Could not initialize biometric prompt")
        }
    }

    /**
     * Prompts the user to unlock the vault using Biometrics.
     */
    fun promptBiometricUnlock(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricEnabled()) {
            onError("Biometric unlock is not enabled")
            return
        }

        try {
            val bytes = secretFile.readBytes()
            if (bytes.size < 17) {
                onError("Corrupted biometric credentials file")
                return
            }
            val ivLen = bytes[0].toInt() and 0xFF
            if (bytes.size < 1 + ivLen + 16) {
                onError("Corrupted biometric credentials file")
                return
            }
            val iv = bytes.copyOfRange(1, 1 + ivLen)
            val ciphertext = bytes.copyOfRange(1 + ivLen, bytes.size)

            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

            val cryptoObject = BiometricPrompt.CryptoObject(cipher)
            val executor = ContextCompat.getMainExecutor(activity)

            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val authenticatedCipher = result.cryptoObject?.cipher
                        if (authenticatedCipher == null) {
                            onError("Biometric cipher missing")
                            return
                        }

                        val masterKey: ByteArray
                        try {
                            masterKey = authenticatedCipher.doFinal(ciphertext)
                        } catch (e: Exception) {
                            Log.e(TAG, "Biometric doFinal failed", e)
                            onError("Failed decrypting biometric secret: ${e.message}")
                            return
                        }

                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            try {
                                encryptedIndex.loadIndex(masterKey)
                                vaultSession.unlock(masterKey)
                                withContext(Dispatchers.Main) {
                                    onSuccess()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed loading index with biometric key", e)
                                withContext(Dispatchers.Main) {
                                    onError("Failed decrypting vault: ${e.message}")
                                }
                            } finally {
                                SecureMemory.wipe(masterKey)
                            }
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Vaulta")
                .setSubtitle("Authenticate using your biometric credentials")
                .setNegativeButtonText("Use Password")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Biometric key invalidated due to new biometric enrollment.")
            disableBiometrics()
            onError("Biometric credentials changed. Please unlock with your password and re-enable biometrics in Settings.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting biometric unlock", e)
            onError(e.message ?: "Biometric prompt failed")
        }
    }

    fun disableBiometrics() {
        try {
            if (secretFile.exists()) secretFile.delete()
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
            Log.i(TAG, "Biometric unlock disabled and credentials purged")
        } catch (e: Exception) {
            Log.w(TAG, "Failed disabling biometrics: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BiometricUnlockMgr"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "VaultaBiometricMasterKey"
    }
}
