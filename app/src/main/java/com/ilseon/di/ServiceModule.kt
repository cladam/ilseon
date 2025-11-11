package com.ilseon.di

import com.ilseon.service.HapticManager
import com.ilseon.service.HapticManagerImpl
import com.ilseon.service.SoundManager
import com.ilseon.service.SoundManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindHapticManager(
        hapticManagerImpl: HapticManagerImpl
    ): HapticManager

    @Binds
    @Singleton
    abstract fun bindSoundManager(
        soundManagerImpl: SoundManagerImpl
    ): SoundManager
}
