package com.vaulta.shrs

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.CachePolicy
import com.vaulta.storage.cleanup.ViewerCachePurge
import com.vaulta.vault.session.AutoLockManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VaultaApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var autoLockManager: AutoLockManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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

        autoLockManager.start()
    }
}
