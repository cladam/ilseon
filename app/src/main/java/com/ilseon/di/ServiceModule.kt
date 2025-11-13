package com.ilseon.di

import com.ilseon.service.HapticManager
import com.ilseon.service.HapticManagerImpl
import com.ilseon.service.NotificationService
import com.ilseon.service.NotificationServiceImpl
import com.ilseon.service.SoundManager
import com.ilseon.service.SoundManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    abstract fun bindSoundManager(
        soundManagerImpl: SoundManagerImpl
    ): SoundManager

    @Binds
    abstract fun bindHapticManager(
        hapticManagerImpl: HapticManagerImpl
    ): HapticManager

    @Binds
    abstract fun bindNotificationService(
        notificationServiceImpl: NotificationServiceImpl
    ): NotificationService
}
