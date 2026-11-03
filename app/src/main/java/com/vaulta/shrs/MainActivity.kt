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
        // Set initial FLAG_SECURE safely
        if (appPreferencesManager.isScreenProtectionEnabled.value) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        enableEdgeToEdge()

        setContent {
            val themeStyle by appPreferencesManager.themeStyle.collectAsStateWithLifecycle()
            val themeMode by appPreferencesManager.themeMode.collectAsStateWithLifecycle()
            val isScreenProtectionEnabled by appPreferencesManager.isScreenProtectionEnabled.collectAsStateWithLifecycle()

            // Update window flag ONLY when its value actually changes to prevent surface recreation
            LaunchedEffect(isScreenProtectionEnabled) {
                val currentFlags = window.attributes.flags
                val isSecure = (currentFlags and WindowManager.LayoutParams.FLAG_SECURE) != 0
                if (isScreenProtectionEnabled && !isSecure) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else if (!isScreenProtectionEnabled && isSecure) {
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
