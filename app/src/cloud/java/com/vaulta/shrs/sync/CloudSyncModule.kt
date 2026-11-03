package com.vaulta.shrs.sync

import com.vaulta.sync.SyncEngine
import com.vaulta.vault.sync.SyncProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CloudSyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncProvider(impl: SyncEngine): SyncProvider
}
