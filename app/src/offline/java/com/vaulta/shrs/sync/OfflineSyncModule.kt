package com.vaulta.shrs.sync

import com.vaulta.vault.sync.SyncProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Singleton
class NoOpSyncProvider : SyncProvider {
    override val isAvailable: Boolean = false
    override val providerName: String = "Offline (Air-Gapped)"

    override suspend fun sync(): Result<Unit> {
        return Result.failure(IllegalStateException("Cloud sync is permanently disabled in the offline air-gapped edition"))
    }
}

@Module
@InstallIn(SingletonComponent::class)
object OfflineSyncModule {

    @Provides
    @Singleton
    fun provideSyncProvider(): SyncProvider = NoOpSyncProvider()
}
