package com.vaulta.shrs.navigation

object Routes {
    const val WELCOME = "welcome"
    const val CREATE_VAULT = "create_vault"
    const val UNLOCK_VAULT = "unlock_vault"
    const val RECOVERY = "recovery"
    const val BROWSER = "browser?parentId={parentId}"
    const val VIEWER = "viewer/{entryId}"
    const val SETTINGS = "settings"
    const val SETTINGS_THEME = "settings/theme"
    const val SETTINGS_SECURITY = "settings/security"
    const val SETTINGS_CREDENTIALS = "settings/credentials"
    const val SETTINGS_ABOUT = "settings/about"

    fun buildBrowserRoute(parentId: String? = null): String {
        return if (parentId != null) "browser?parentId=$parentId" else "browser"
    }

    fun buildViewerRoute(entryId: String): String {
        return "viewer/$entryId"
    }
}
