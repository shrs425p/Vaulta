package com.vaulta.shrs

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.CachePolicy
import com.vaulta.storage.cleanup.ViewerCachePurge
import com.vaulta.vault.session.AutoLockManager
import javax.inject.Inject

abstract class BaseVaultaApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var autoLockManager: AutoLockManager

    override fun newImageLoader(): ImageLoader {
        // Zero-Knowledge Cache Policy: Disable Coil disk caching completely so decrypted
        // image previews and thumbnails are NEVER written to flash storage.
        return ImageLoader.Builder(this)
            .diskCachePolicy(CachePolicy.DISABLED)
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("Vaulta", "Vaulta Application initialized successfully")

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("VaultaCrash", "💥 FATAL UNCAUGHT EXCEPTION in thread '${thread.name}'", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Clean up any stale decrypted viewer and shared cache from previous ungraceful exits/crashes
        ViewerCachePurge.purgeViewerTemp(this)

        // Initialize user-activity & lifecycle watcher for auto-lock
        autoLockManager.start()
    }
}
