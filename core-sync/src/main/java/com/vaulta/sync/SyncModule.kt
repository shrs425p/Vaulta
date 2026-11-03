package com.vaulta.sync

import com.vaulta.sync.drive.DriveSyncClient
import com.vaulta.sync.drive.DriveSyncClientImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindDriveSyncClient(
        impl: DriveSyncClientImpl
    ): DriveSyncClient
}
