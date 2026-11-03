package com.vaulta.vault.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class KeyFilePreferenceManagerTest {

    private lateinit var mockContext: Context
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var manager: KeyFilePreferenceManager

    @Before
    fun setUp() {
        mockContext = mockk<Context>()
        fakePrefs = FakeSharedPreferences()
        every { mockContext.getSharedPreferences(any(), any()) } returns fakePrefs

        manager = KeyFilePreferenceManager(mockContext)
    }

    @Test
    fun failedAttempts_triggersLockout_andResetsOnSuccess() {
        assertThat(manager.getFailedUnlockAttempts()).isEqualTo(0)
        assertThat(manager.getLockoutRemainingSeconds()).isEqualTo(0L)

        // Attempt 1 & 2 -> No lockout delay yet
        assertThat(manager.recordFailedUnlockAttempt()).isEqualTo(0L)
        assertThat(manager.recordFailedUnlockAttempt()).isEqualTo(0L)

        // Attempt 3 -> 5 second lockout
        val lockout3 = manager.recordFailedUnlockAttempt()
        assertThat(lockout3).isEqualTo(5L)
        assertThat(manager.getLockoutRemainingSeconds()).isGreaterThan(0L)

        // Reset
        manager.resetFailedUnlockAttempts()
        assertThat(manager.getFailedUnlockAttempts()).isEqualTo(0)
        assertThat(manager.getLockoutRemainingSeconds()).isEqualTo(0L)
    }
}

class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = data

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        data[key] as? MutableSet<String> ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        data[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        data[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        data[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(data)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val data: MutableMap<String, Any>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any>()
        private val removed = mutableSetOf<String>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null && value != null) temp[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null && values != null) temp[key] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removed.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            data.clear()
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            removed.forEach { data.remove(it) }
            data.putAll(temp)
        }
    }
}
