package com.vaulta.shrs.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vaulta.browser.BrowserScreen
import com.vaulta.shrs.BuildConfig
import com.vaulta.onboarding.create.CreateVaultScreen
import com.vaulta.onboarding.recovery.RecoveryScreen
import com.vaulta.onboarding.unlock.UnlockVaultScreen
import com.vaulta.onboarding.welcome.WelcomeScreen
import com.vaulta.settings.AboutScreen
import com.vaulta.settings.CredentialsSettingsScreen
import com.vaulta.settings.SecuritySettingsScreen
import com.vaulta.settings.SettingsScreen
import com.vaulta.settings.ThemeSettingsScreen
import com.vaulta.vault.session.VaultSession
import com.vaulta.viewer.ViewerScreen

@Composable
fun VaultaNavHost(
    vaultSession: VaultSession = hiltViewModel<NavViewModel>().vaultSession
) {
    val navController = rememberNavController()
    val isUnlocked by vaultSession.isUnlocked.collectAsStateWithLifecycle()

    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != null && currentRoute != Routes.WELCOME && currentRoute != Routes.UNLOCK_VAULT && currentRoute != Routes.CREATE_VAULT && currentRoute != Routes.RECOVERY) {
                navController.navigate(Routes.WELCOME) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    val enterAnimDuration = 220
    val exitAnimDuration = 180

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 6 },
                animationSpec = tween(enterAnimDuration, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(enterAnimDuration, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 10 },
                animationSpec = tween(exitAnimDuration, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(exitAnimDuration, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 10 },
                animationSpec = tween(enterAnimDuration, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(enterAnimDuration, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 6 },
                animationSpec = tween(exitAnimDuration, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(exitAnimDuration, easing = FastOutSlowInEasing))
        }
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onCreateVaultClick = { navController.navigate(Routes.CREATE_VAULT) },
                onUnlockVaultClick = { navController.navigate(Routes.UNLOCK_VAULT) },
                onRecoveryClick = { navController.navigate(Routes.RECOVERY) },
                isOfflineEdition = BuildConfig.IS_OFFLINE
            )
        }

        composable(Routes.CREATE_VAULT) {
            CreateVaultScreen(
                onNavigateBack = { navController.popBackStack() },
                onVaultCreated = {
                    navController.navigate(Routes.buildBrowserRoute(null)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.UNLOCK_VAULT) {
            UnlockVaultScreen(
                onNavigateBack = { navController.popBackStack() },
                onUnlocked = {
                    navController.navigate(Routes.buildBrowserRoute(null)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.RECOVERY) {
            RecoveryScreen(
                onNavigateBack = { navController.popBackStack() },
                onRecovered = {
                    navController.navigate(Routes.buildBrowserRoute(null)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.BROWSER,
            arguments = listOf(
                navArgument("parentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val parentId = backStackEntry.arguments?.getString("parentId")
            BrowserScreen(
                parentId = parentId,
                onNavigateToFolder = { folderId ->
                    navController.navigate(Routes.buildBrowserRoute(folderId))
                },
                onNavigateBack = { navController.popBackStack() },
                onOpenFile = { entryId ->
                    navController.navigate(Routes.buildViewerRoute(entryId))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("entryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId").orEmpty()
            ViewerScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLockVault = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToTheme = { navController.navigate(Routes.SETTINGS_THEME) },
                onNavigateToSecurity = { navController.navigate(Routes.SETTINGS_SECURITY) },
                onNavigateToCredentials = { navController.navigate(Routes.SETTINGS_CREDENTIALS) },
                onNavigateToAbout = { navController.navigate(Routes.SETTINGS_ABOUT) }
            )
        }

        composable(Routes.SETTINGS_THEME) {
            ThemeSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }

        composable(Routes.SETTINGS_SECURITY) {
            SecuritySettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }

        composable(Routes.SETTINGS_CREDENTIALS) {
            CredentialsSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }

        composable(Routes.SETTINGS_ABOUT) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
    }
}
