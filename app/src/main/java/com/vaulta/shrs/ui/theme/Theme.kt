package com.vaulta.shrs.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.vaulta.vault.preferences.AppThemeMode
import com.vaulta.vault.preferences.AppThemeStyle

private fun softLightColorScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    surface: Color,
    surfaceVariant: Color
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF191C1E),
    surface = surface,
    onSurface = Color(0xFF191C1E),
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC4C7D0)
)

private fun softDarkColorScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    surface: Color,
    surfaceVariant: Color
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    background = Color(0xFF121417),
    onBackground = Color(0xFFE1E3E6),
    surface = surface,
    onSurface = Color(0xFFE1E3E6),
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = Color(0xFFC4C7CC),
    outline = Color(0xFF8E939C),
    outlineVariant = Color(0xFF323740)
)

// 1. Monochrome (True AMOLED Black & White)
private val MonochromeDark = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2B2B2B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFD4D4D4),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF222222),
    onSecondaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF161616),
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFF666666),
    outlineVariant = Color(0xFF333333)
)

private val MonochromeLight = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E4E4),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF404040),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEDED),
    onSecondaryContainer = Color(0xFF111111),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF333333),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFDCDCDC)
)

@Composable
fun VaultaTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.DYNAMIC,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when (themeStyle) {
        AppThemeStyle.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) {
                    softDarkColorScheme(
                        primary = Color(0xFF4DB6AC), onPrimary = Color(0xFF003732),
                        primaryContainer = Color(0xFF005049), onPrimaryContainer = Color(0xFF70F7E7),
                        secondary = Color(0xFF81D4FA), onSecondary = Color(0xFF00344D),
                        secondaryContainer = Color(0xFF004C6E), onSecondaryContainer = Color(0xFFC3E7FF),
                        surface = Color(0xFF121417), surfaceVariant = Color(0xFF252930)
                    )
                } else {
                    softLightColorScheme(
                        primary = Color(0xFF006A61), onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFF70F7E7), onPrimaryContainer = Color(0xFF00201D),
                        secondary = Color(0xFF006492), onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFFC3E7FF), onSecondaryContainer = Color(0xFF001E2F),
                        surface = Color(0xFFFCFCFF), surfaceVariant = Color(0xFFEFF1F3)
                    )
                }
            }
        }

        AppThemeStyle.MONOCHROME -> {
            if (isDark) MonochromeDark else MonochromeLight
        }

        AppThemeStyle.LAVENDER_SOFT -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
                    primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
                    secondary = Color(0xFFCCC2DC), onSecondary = Color(0xFF332D41),
                    secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
                    surface = Color(0xFF141218), surfaceVariant = Color(0xFF2B2831)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF6750A4), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
                    secondary = Color(0xFF625B71), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
                    surface = Color(0xFFFEF7FF), surfaceVariant = Color(0xFFE7E0EB)
                )
            }
        }

        AppThemeStyle.SAGE_GARDEN -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFFB1D18A), onPrimary = Color(0xFF1F3701),
                    primaryContainer = Color(0xFF354E16), onPrimaryContainer = Color(0xFFCDEDA3),
                    secondary = Color(0xFFBFCBAD), onSecondary = Color(0xFF2A331E),
                    secondaryContainer = Color(0xFF404A33), onSecondaryContainer = Color(0xFFDCE7C8),
                    surface = Color(0xFF12140E), surfaceVariant = Color(0xFF292C24)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF4C662B), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFCDEDA3), onPrimaryContainer = Color(0xFF102000),
                    secondary = Color(0xFF586249), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFDCE7C8), onSecondaryContainer = Color(0xFF151E0B),
                    surface = Color(0xFFF9FAEF), surfaceVariant = Color(0xFFE1E4D5)
                )
            }
        }

        AppThemeStyle.OCEAN_MIST -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFF7BD0FF), onPrimary = Color(0xFF00354A),
                    primaryContainer = Color(0xFF004D69), onPrimaryContainer = Color(0xFFC2E8FF),
                    secondary = Color(0xFFB5C9D7), onSecondary = Color(0xFF20333E),
                    secondaryContainer = Color(0xFF374955), onSecondaryContainer = Color(0xFFD1E5F4),
                    surface = Color(0xFF0E1418), surfaceVariant = Color(0xFF242B30)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF00668B), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFC2E8FF), onPrimaryContainer = Color(0xFF001E2C),
                    secondary = Color(0xFF4E616D), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFD1E5F4), onSecondaryContainer = Color(0xFF0A1E28),
                    surface = Color(0xFFF6FAFE), surfaceVariant = Color(0xFFDCE4E9)
                )
            }
        }

        AppThemeStyle.DUSTY_ROSE -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFFFFB2BF), onPrimary = Color(0xFF561D2D),
                    primaryContainer = Color(0xFF723443), onPrimaryContainer = Color(0xFFFFD9DF),
                    secondary = Color(0xFFE5BDC2), onSecondary = Color(0xFF43292E),
                    secondaryContainer = Color(0xFF5B3F44), onSecondaryContainer = Color(0xFFFFD9DF),
                    surface = Color(0xFF191113), surfaceVariant = Color(0xFF302527)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF8F4C5A), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFD9DF), onPrimaryContainer = Color(0xFF3A0718),
                    secondary = Color(0xFF75565C), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFFD9DF), onSecondaryContainer = Color(0xFF2B151A),
                    surface = Color(0xFFFFF8F8), surfaceVariant = Color(0xFFF2DDE0)
                )
            }
        }

        AppThemeStyle.PEACH_SUNSET -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFFFFB596), onPrimary = Color(0xFF542103),
                    primaryContainer = Color(0xFF703717), onPrimaryContainer = Color(0xFFFFDBCF),
                    secondary = Color(0xFFE7BDB0), onSecondary = Color(0xFF442A1F),
                    secondaryContainer = Color(0xFF5D4034), onSecondaryContainer = Color(0xFFFFDBCF),
                    surface = Color(0xFF19120E), surfaceVariant = Color(0xFF302520)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF8D4E2C), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFDBCF), onPrimaryContainer = Color(0xFF370E00),
                    secondary = Color(0xFF77574A), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFFDBCF), onSecondaryContainer = Color(0xFF2C160C),
                    surface = Color(0xFFFFF8F6), surfaceVariant = Color(0xFFF5DED6)
                )
            }
        }

        AppThemeStyle.HONEY_AMBER -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFFF2C236), onPrimary = Color(0xFF3E2E00),
                    primaryContainer = Color(0xFF594400), onPrimaryContainer = Color(0xFFFFDF8B),
                    secondary = Color(0xFFD6C5A1), onSecondary = Color(0xFF3A3016),
                    secondaryContainer = Color(0xFF52462A), onSecondaryContainer = Color(0xFFF3E1BB),
                    surface = Color(0xFF16130B), surfaceVariant = Color(0xFF2C271D)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF765B00), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFDF8B), onPrimaryContainer = Color(0xFF241A00),
                    secondary = Color(0xFF6A5D3F), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF3E1BB), onSecondaryContainer = Color(0xFF231B04),
                    surface = Color(0xFFFFF9EE), surfaceVariant = Color(0xFFEAE2CF)
                )
            }
        }

        AppThemeStyle.MINT_BREEZE -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFF52DBCB), onPrimary = Color(0xFF003731),
                    primaryContainer = Color(0xFF005048), onPrimaryContainer = Color(0xFF73F8E7),
                    secondary = Color(0xFFB0CCC6), onSecondary = Color(0xFF1C3531),
                    secondaryContainer = Color(0xFF324B47), onSecondaryContainer = Color(0xFFCCE8E2),
                    surface = Color(0xFF0E1514), surfaceVariant = Color(0xFF232D2B)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF006A60), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFF73F8E7), onPrimaryContainer = Color(0xFF00201C),
                    secondary = Color(0xFF4A635F), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFCCE8E2), onSecondaryContainer = Color(0xFF05201C),
                    surface = Color(0xFFF4FBF8), surfaceVariant = Color(0xFFDAE5E1)
                )
            }
        }

        AppThemeStyle.NORDIC_FROST -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFF59D5F8), onPrimary = Color(0xFF003642),
                    primaryContainer = Color(0xFF004E5E), onPrimaryContainer = Color(0xFFB1ECFF),
                    secondary = Color(0xFFB2CBD3), onSecondary = Color(0xFF1D343A),
                    secondaryContainer = Color(0xFF344A51), onSecondaryContainer = Color(0xFFCEE7EF),
                    surface = Color(0xFF0E1416), surfaceVariant = Color(0xFF232C2E)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF00677C), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFB1ECFF), onPrimaryContainer = Color(0xFF001F27),
                    secondary = Color(0xFF4B6269), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFCEE7EF), onSecondaryContainer = Color(0xFF061E25),
                    surface = Color(0xFFF5FAFC), surfaceVariant = Color(0xFFDBE4E7)
                )
            }
        }

        AppThemeStyle.MOCHA_CREAM -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFFE8BEAC), onPrimary = Color(0xFF45291C),
                    primaryContainer = Color(0xFF5E3F30), onPrimaryContainer = Color(0xFFFFDBCF),
                    secondary = Color(0xFFD7C2B9), onSecondary = Color(0xFF3A2D26),
                    secondaryContainer = Color(0xFF52433C), onSecondaryContainer = Color(0xFFF3DFD6),
                    surface = Color(0xFF171210), surfaceVariant = Color(0xFF2D2522)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF785646), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFDBCF), onPrimaryContainer = Color(0xFF2C1509),
                    secondary = Color(0xFF6A5A53), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF3DFD6), onSecondaryContainer = Color(0xFF241812),
                    surface = Color(0xFFFFF8F6), surfaceVariant = Color(0xFFEFE0DA)
                )
            }
        }

        AppThemeStyle.DEEP_SLATE -> {
            if (isDark) {
                softDarkColorScheme(
                    primary = Color(0xFFB3CAD6), onPrimary = Color(0xFF1E333C),
                    primaryContainer = Color(0xFF344953), onPrimaryContainer = Color(0xFFCFE6F3),
                    secondary = Color(0xFFBBC8D0), onSecondary = Color(0xFF253138),
                    secondaryContainer = Color(0xFF3B484F), onSecondaryContainer = Color(0xFFD6E3EC),
                    surface = Color(0xFF101416), surfaceVariant = Color(0xFF262C30)
                )
            } else {
                softLightColorScheme(
                    primary = Color(0xFF4C616C), onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFCFE6F3), onPrimaryContainer = Color(0xFF061E27),
                    secondary = Color(0xFF535F67), onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFD6E3EC), onSecondaryContainer = Color(0xFF101C23),
                    surface = Color(0xFFF7F9FB), surfaceVariant = Color(0xFFDCE3E8)
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = VaultaShapes,
        content = content
    )
}
