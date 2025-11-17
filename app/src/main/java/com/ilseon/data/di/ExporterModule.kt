package com.ilseon.data.di

import com.ilseon.data.task.NoteExporter
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
    fun provideNoteExporter(): NoteExporter {
        return NoteExporter()
    }
}
