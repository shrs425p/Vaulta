package com.vaulta.vault

import com.google.common.truth.Truth.assertThat
import com.vaulta.crypto.Argon2Params
import com.vaulta.vault.keyfile.InvalidVaultKeyFileException
import com.vaulta.vault.keyfile.VaultKeyFile
import com.vaulta.vault.keyfile.VaultKeyFileSerializer
import com.vaulta.vault.keyfile.VaultKeyFileSerializerImpl
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class VaultKeyFileSerializerTest {

    private lateinit var serializer: VaultKeyFileSerializer

    @Before
    fun setUp() {
        serializer = VaultKeyFileSerializerImpl()
    }

    @Test
    fun roundTrip_serialization_matchesOriginal() {
        val original = VaultKeyFile(
            argon2Params = Argon2Params(memoryCostKb = 65536, iterations = 3, parallelism = 4),
            salt = ByteArray(32) { it.toByte() },
            wrappedMasterKeySlotA = ByteArray(48) { (it * 2).toByte() },
            wrappedMasterKeySlotB = ByteArray(48) { (it * 3).toByte() }
        )

        val serialized = serializer.serialize(original)
        val deserialized = serializer.deserialize(serialized)

        assertThat(deserialized).isEqualTo(original)
    }

    @Test
    fun deserialize_badMagic_throwsInvalidVaultKeyFileException() {
        val corrupted = "NOTVAL".toByteArray(Charsets.US_ASCII) + ByteArray(100)

        assertThrows(InvalidVaultKeyFileException::class.java) {
            serializer.deserialize(corrupted)
        }
    }

    @Test
    fun deserialize_lowArgon2Memory_throwsInvalidVaultKeyFileException() {
        val weak = VaultKeyFile(
            argon2Params = Argon2Params(memoryCostKb = 8192, iterations = 3, parallelism = 4), // Below 16MB minimum
            salt = ByteArray(32) { it.toByte() },
            wrappedMasterKeySlotA = ByteArray(48) { 1 },
            wrappedMasterKeySlotB = ByteArray(48) { 2 }
        )

        val serialized = serializer.serialize(weak)
        assertThrows(InvalidVaultKeyFileException::class.java) {
            serializer.deserialize(serialized)
        }
    }

    @Test
    fun deserialize_zeroArgon2Iterations_throwsInvalidVaultKeyFileException() {
        val weak = VaultKeyFile(
            argon2Params = Argon2Params(memoryCostKb = 65536, iterations = 0, parallelism = 4), // Below 1 iteration
            salt = ByteArray(32) { it.toByte() },
            wrappedMasterKeySlotA = ByteArray(48) { 1 },
            wrappedMasterKeySlotB = ByteArray(48) { 2 }
        )

        val serialized = serializer.serialize(weak)
        assertThrows(InvalidVaultKeyFileException::class.java) {
            serializer.deserialize(serialized)
        }
    }

    @Test
    fun deserialize_excessiveArgon2Parameters_throwsInvalidVaultKeyFileException() {
        val oversized = VaultKeyFile(
            argon2Params = Argon2Params(
                memoryCostKb = VaultKeyFileSerializerImpl.MAX_ARGON2_MEMORY_KB + 1,
                iterations = VaultKeyFileSerializerImpl.MAX_ARGON2_ITERATIONS + 1,
                parallelism = VaultKeyFileSerializerImpl.MAX_ARGON2_PARALLELISM + 1
            ),
            salt = ByteArray(32),
            wrappedMasterKeySlotA = ByteArray(48),
            wrappedMasterKeySlotB = ByteArray(48)
        )

        assertThrows(InvalidVaultKeyFileException::class.java) {
            serializer.deserialize(serializer.serialize(oversized))
        }
    }
}
