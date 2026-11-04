package com.vaulta.vault.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class DefaultSyncPolicy(val displayName: String, val description: String) {
    SYNC_ALL("Sync All Files Automatically", "Newly imported files are uploaded to Google Drive during sync runs."),
    SELECTIVE_MANUAL("Only Sync Selected Files", "Newly imported files remain local-only until explicitly marked for cloud upload.")
}

enum class AppThemeStyle(
    val displayName: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val description: String
) {
    DYNAMIC(
        displayName = "Dynamic Material UI",
        primaryColorHex = 0xFF00897B,
        secondaryColorHex = 0xFF0288D1,
        description = "Material You extracted from device wallpaper"
    ),
    MONOCHROME(
        displayName = "Black & White",
        primaryColorHex = 0xFF000000,
        secondaryColorHex = 0xFF555555,
        description = "Pure minimalist monochrome & true AMOLED black"
    ),
    LAVENDER_SOFT(
        displayName = "Lavender Soft",
        primaryColorHex = 0xFF7E57C2,
        secondaryColorHex = 0xFFB39DDB,
        description = "Soft calming lilac & lavender"
    ),
    SAGE_GARDEN(
        displayName = "Sage Garden",
        primaryColorHex = 0xFF558B2F,
        secondaryColorHex = 0xFF9CCC65,
        description = "Earthy muted botanical sage"
    ),
    OCEAN_MIST(
        displayName = "Ocean Mist",
        primaryColorHex = 0xFF0288D1,
        secondaryColorHex = 0xFF81D4FA,
        description = "Soft coastal slate & maritime sky"
    ),
    DUSTY_ROSE(
        displayName = "Dusty Rose",
        primaryColorHex = 0xFFC2185B,
        secondaryColorHex = 0xFFF48FB1,
        description = "Warm muted vintage rose"
    ),
    PEACH_SUNSET(
        displayName = "Peach Sunset",
        primaryColorHex = 0xFFE65100,
        secondaryColorHex = 0xFFFFB74D,
        description = "Warm gentle pastel peach & coral"
    ),
    HONEY_AMBER(
        displayName = "Honey Amber",
        primaryColorHex = 0xFFF57F17,
        secondaryColorHex = 0xFFFFE082,
        description = "Warm soothing golden honey"
    ),
    MINT_BREEZE(
        displayName = "Mint Breeze",
        primaryColorHex = 0xFF00897B,
        secondaryColorHex = 0xFF80CBC4,
        description = "Fresh soft mint & eucalyptus"
    ),
    NORDIC_FROST(
        displayName = "Nordic Frost",
        primaryColorHex = 0xFF00838F,
        secondaryColorHex = 0xFF80DEEA,
        description = "Crisp arctic cyan & pale glacier"
    ),
    MOCHA_CREAM(
        displayName = "Mocha Cream",
        primaryColorHex = 0xFF6D4C41,
        secondaryColorHex = 0xFFBCAAA4,
        description = "Warm roasted mocha & oat cream"
    ),
    DEEP_SLATE(
        displayName = "Deep Slate",
        primaryColorHex = 0xFF546E7A,
        secondaryColorHex = 0xFFB0BEC5,
        description = "Modern minimal graphite & silver slate"
    )
}

@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("vaulta_app_prefs", Context.MODE_PRIVATE)
    }

    private val _isScreenProtectionEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SCREEN_PROTECTION, true)
    )
    val isScreenProtectionEnabled: StateFlow<Boolean> = _isScreenProtectionEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _themeStyle = MutableStateFlow(
        try {
            AppThemeStyle.valueOf(prefs.getString(KEY_THEME_STYLE, AppThemeStyle.DYNAMIC.name) ?: AppThemeStyle.DYNAMIC.name)
        } catch (_: Exception) {
            AppThemeStyle.DYNAMIC
        }
    )
    val themeStyle: StateFlow<AppThemeStyle> = _themeStyle.asStateFlow()

    private val _autoLockTimeoutMinutes = MutableStateFlow(
        prefs.getInt(KEY_AUTO_LOCK_TIMEOUT_MINUTES, 5)
    )
    val autoLockTimeoutMinutes: StateFlow<Int> = _autoLockTimeoutMinutes.asStateFlow()

    private val _deleteOriginalAfterImport = MutableStateFlow(
        prefs.getBoolean(KEY_DELETE_ORIGINAL_AFTER_IMPORT, false)
    )
    val deleteOriginalAfterImport: StateFlow<Boolean> = _deleteOriginalAfterImport.asStateFlow()

    private val _isStealthSyncEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_STEALTH_SYNC_ENABLED, false)
    )
    val isStealthSyncEnabled: StateFlow<Boolean> = _isStealthSyncEnabled.asStateFlow()

    private val _defaultSyncPolicy = MutableStateFlow(
        try {
            DefaultSyncPolicy.valueOf(prefs.getString(KEY_DEFAULT_SYNC_POLICY, DefaultSyncPolicy.SYNC_ALL.name) ?: DefaultSyncPolicy.SYNC_ALL.name)
        } catch (_: Exception) {
            DefaultSyncPolicy.SYNC_ALL
        }
    )
    val defaultSyncPolicy: StateFlow<DefaultSyncPolicy> = _defaultSyncPolicy.asStateFlow()

    fun setAutoLockTimeoutMinutes(minutes: Int) {
        _autoLockTimeoutMinutes.value = minutes
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT_MINUTES, minutes).apply()
    }

    fun setDeleteOriginalAfterImport(enabled: Boolean) {
        _deleteOriginalAfterImport.value = enabled
        prefs.edit().putBoolean(KEY_DELETE_ORIGINAL_AFTER_IMPORT, enabled).apply()
    }

    fun setIsStealthSyncEnabled(enabled: Boolean) {
        _isStealthSyncEnabled.value = enabled
        prefs.edit().putBoolean(KEY_STEALTH_SYNC_ENABLED, enabled).apply()
    }

    fun setDefaultSyncPolicy(policy: DefaultSyncPolicy) {
        _defaultSyncPolicy.value = policy
        prefs.edit().putString(KEY_DEFAULT_SYNC_POLICY, policy.name).apply()
    }

    fun setScreenProtectionEnabled(enabled: Boolean) {
        _isScreenProtectionEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SCREEN_PROTECTION, enabled).apply()
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setThemeStyle(style: AppThemeStyle) {
        _themeStyle.value = style
        prefs.edit().putString(KEY_THEME_STYLE, style.name).apply()
    }

    companion object {
        private const val KEY_SCREEN_PROTECTION = "screen_protection_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_STYLE = "theme_style"
        private const val KEY_AUTO_LOCK_TIMEOUT_MINUTES = "auto_lock_timeout_minutes"
        private const val KEY_DELETE_ORIGINAL_AFTER_IMPORT = "delete_original_after_import"
        private const val KEY_STEALTH_SYNC_ENABLED = "stealth_sync_enabled"
        private const val KEY_DEFAULT_SYNC_POLICY = "default_sync_policy"
    }
}
