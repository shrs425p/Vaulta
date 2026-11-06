package com.vaulta.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Reads the version configured for the installed app variant, including its flavor suffix. */
internal fun getInstalledAppVersionName(context: Context): String {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    return packageInfo.versionName ?: "Unknown"
}
