package com.ilseon.di

import android.content.Context
import com.ilseon.data.bluetooth.BluetoothChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideBluetoothChecker(@ApplicationContext context: Context): BluetoothChecker {
        return BluetoothChecker(context)
    }
}
