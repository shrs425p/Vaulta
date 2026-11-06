package com.vaulta.shrs

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaulta.shrs.navigation.VaultaNavHost
import com.vaulta.shrs.ui.theme.VaultaTheme
import com.vaulta.vault.preferences.AppPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appPreferencesManager: AppPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply protection before the first frame can be drawn or captured.
        if (appPreferencesManager.isScreenProtectionEnabled.value) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        enableEdgeToEdge()

        setContent {
            val isScreenProtectionEnabled by appPreferencesManager.isScreenProtectionEnabled.collectAsStateWithLifecycle()
            val themeStyle by appPreferencesManager.themeStyle.collectAsStateWithLifecycle()
            val themeMode by appPreferencesManager.themeMode.collectAsStateWithLifecycle()
            LaunchedEffect(isScreenProtectionEnabled) {
                if (isScreenProtectionEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            VaultaTheme(
                themeStyle = themeStyle,
                themeMode = themeMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VaultaNavHost()
                }
            }
        }
    }
}
