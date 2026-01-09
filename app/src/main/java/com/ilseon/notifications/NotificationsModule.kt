package com.ilseon.notifications

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    abstract fun bindFuelCheckScheduler(
        workManagerFuelCheckScheduler: WorkManagerFuelCheckScheduler
    ): FuelCheckScheduler
}
