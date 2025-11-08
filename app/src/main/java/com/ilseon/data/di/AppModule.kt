package com.ilseon.data.di

import android.content.Context
import androidx.room.Room
import com.ilseon.AppDatabase
import com.ilseon.data.task.TaskContextDao
import com.ilseon.data.task.TaskContextRepository
import com.ilseon.data.task.TaskDao
import com.ilseon.data.task.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ilseon_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepository(taskDao)
    }

    @Provides
    fun provideTaskContextDao(appDatabase: com.ilseon.AppDatabase): TaskContextDao {
        return appDatabase.taskContextDao()
    }

    @Provides
    @Singleton
    fun provideTaskContextRepository(taskContextDao: TaskContextDao): TaskContextRepository {
        return TaskContextRepository(taskContextDao)
    }
}
