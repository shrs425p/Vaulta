package com.vaulta.shrs

import android.app.Application
import com.vaulta.storage.cleanup.ViewerCachePurge
import com.vaulta.vault.clipboard.ClipboardAutoPurgeManager
import com.vaulta.vault.session.AutoLockManager
import javax.inject.Inject

abstract class BaseVaultaApp : Application() {

    @Inject
    lateinit var autoLockManager: AutoLockManager

    @Inject
    lateinit var clipboardAutoPurgeManager: ClipboardAutoPurgeManager

    override fun onCreate() {
        super.onCreate()

        clipboardAutoPurgeManager.clearOwnedClip()
        ViewerCachePurge.purgeViewerTemp(this)

        // Initialize user-activity & lifecycle watcher for auto-lock
        autoLockManager.start()
    }
}
