package com.vaulta.vault.keyfile

import com.vaulta.crypto.Argon2Params
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles serialization and deserialization of [VaultKeyFile] to/from raw bytes.
 */
interface VaultKeyFileSerializer {

    /**
     * Serializes a [VaultKeyFile] to a [ByteArray].
     *
     * Binary format:
     * - 6 bytes: magic string ("VAULTA")
     * - 1 byte:  version
     * - 4 bytes: Argon2 memoryCostKb (Int)
     * - 4 bytes: Argon2 iterations (Int)
     * - 4 bytes: Argon2 parallelism (Int)
     * - 32 bytes: salt
     * - 4 bytes: slot A length
     * - N bytes: slot A data
     * - 4 bytes: slot B length
     * - M bytes: slot B data
     */
    fun serialize(keyFile: VaultKeyFile): ByteArray

    /**
     * Deserializes a [ByteArray] back into a [VaultKeyFile].
     *
     * @throws InvalidVaultKeyFileException if the magic header or version is invalid,
     *         or if the data is otherwise corrupted.
     */
    fun deserialize(data: ByteArray): VaultKeyFile
}

/**
 * Default implementation of [VaultKeyFileSerializer] using [DataOutputStream]/[DataInputStream].
 */
@Singleton
class VaultKeyFileSerializerImpl @Inject constructor() : VaultKeyFileSerializer {

    override fun serialize(keyFile: VaultKeyFile): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { dos ->
            // Magic (6 bytes, written as raw ASCII)
            val magicBytes = keyFile.magic.toByteArray(Charsets.US_ASCII)
            dos.write(magicBytes)

            // Version (1 byte)
            dos.writeByte(keyFile.version)

            // Argon2 params
            dos.writeInt(keyFile.argon2Params.memoryCostKb)
            dos.writeInt(keyFile.argon2Params.iterations)
            dos.writeInt(keyFile.argon2Params.parallelism)

            // Salt (32 bytes)
            dos.write(keyFile.salt)

            // Slot A: length-prefixed
            dos.writeInt(keyFile.wrappedMasterKeySlotA.size)
            dos.write(keyFile.wrappedMasterKeySlotA)

            // Slot B: length-prefixed
            dos.writeInt(keyFile.wrappedMasterKeySlotB.size)
            dos.write(keyFile.wrappedMasterKeySlotB)
        }
        return baos.toByteArray()
    }

    override fun deserialize(data: ByteArray): VaultKeyFile {
        try {
            val dis = DataInputStream(ByteArrayInputStream(data))

            // Magic (6 bytes)
            val magicBytes = ByteArray(VaultKeyFile.MAGIC.length)
            dis.readFully(magicBytes)
            val magic = String(magicBytes, Charsets.US_ASCII)
            if (magic != VaultKeyFile.MAGIC) {
                throw InvalidVaultKeyFileException(
                    "Invalid magic header: expected '${VaultKeyFile.MAGIC}', got '$magic'"
                )
            }

            // Version (1 byte)
            val version = dis.readByte().toInt() and 0xFF
            if (version != VaultKeyFile.CURRENT_VERSION) {
                throw InvalidVaultKeyFileException(
                    "Unsupported vault.key version: expected ${VaultKeyFile.CURRENT_VERSION}, got $version"
                )
            }

            // Argon2 params
            val memoryCostKb = dis.readInt()
            val iterations = dis.readInt()
            val parallelism = dis.readInt()

            if (memoryCostKb < MIN_ARGON2_MEMORY_KB) {
                throw InvalidVaultKeyFileException(
                    "Insecure Argon2 memory cost: $memoryCostKb KB (minimum required: $MIN_ARGON2_MEMORY_KB KB)"
                )
            }
            if (iterations < MIN_ARGON2_ITERATIONS) {
                throw InvalidVaultKeyFileException(
                    "Insecure Argon2 iteration count: $iterations (minimum required: $MIN_ARGON2_ITERATIONS)"
                )
            }
            if (parallelism < MIN_ARGON2_PARALLELISM) {
                throw InvalidVaultKeyFileException(
                    "Insecure Argon2 parallelism: $parallelism (minimum required: $MIN_ARGON2_PARALLELISM)"
                )
            }

            val argon2Params = Argon2Params(
                memoryCostKb = memoryCostKb,
                iterations = iterations,
                parallelism = parallelism,
            )

            // Salt (32 bytes)
            val salt = ByteArray(SALT_SIZE)
            dis.readFully(salt)

            // Slot A
            val slotALength = dis.readInt()
            if (slotALength < 0 || slotALength > MAX_SLOT_SIZE) {
                throw InvalidVaultKeyFileException(
                    "Invalid slot A length: $slotALength"
                )
            }
            val slotA = ByteArray(slotALength)
            dis.readFully(slotA)

            // Slot B
            val slotBLength = dis.readInt()
            if (slotBLength < 0 || slotBLength > MAX_SLOT_SIZE) {
                throw InvalidVaultKeyFileException(
                    "Invalid slot B length: $slotBLength"
                )
            }
            val slotB = ByteArray(slotBLength)
            dis.readFully(slotB)

            return VaultKeyFile(
                magic = magic,
                version = version,
                argon2Params = argon2Params,
                salt = salt,
                wrappedMasterKeySlotA = slotA,
                wrappedMasterKeySlotB = slotB,
            )
        } catch (e: InvalidVaultKeyFileException) {
            throw e
        } catch (e: Exception) {
            throw InvalidVaultKeyFileException(
                "Failed to deserialize vault.key file: ${e.message}",
                e,
            )
        }
    }

    companion object {
        /** Expected salt size in bytes. */
        const val SALT_SIZE = 32

        /** Maximum allowed wrapped key slot size to guard against corrupted data. */
        const val MAX_SLOT_SIZE = 1024

        /** Minimum acceptable Argon2 memory cost to prevent KDF parameter downgrade attacks (16 MB). */
        const val MIN_ARGON2_MEMORY_KB = 16384

        /** Minimum acceptable Argon2 iteration count. */
        const val MIN_ARGON2_ITERATIONS = 1

        /** Minimum acceptable Argon2 parallelism lane count. */
        const val MIN_ARGON2_PARALLELISM = 1
    }
}
