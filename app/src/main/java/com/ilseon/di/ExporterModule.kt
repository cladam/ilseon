package com.ilseon.di

import com.ilseon.data.task.ReflectionExporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExporterModule {

    @Provides
    @Singleton
    fun provideReflectionExporter(): ReflectionExporter {
        return ReflectionExporter()
    }
}
