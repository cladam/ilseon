package com.ilseon.di

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import com.ilseon.AppDatabase
import com.ilseon.DatabaseCallback
import com.ilseon.data.task.TaskContextDao
import com.ilseon.data.task.TaskContextRepository
import com.ilseon.data.task.TaskDao
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.task.FocusBlockDao
import com.ilseon.notifications.ReminderManager
import com.ilseon.service.HapticManager
import com.ilseon.service.HapticManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindHapticManager(impl: HapticManagerImpl): HapticManager

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context,
            callback: DatabaseCallback
        ): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "ilseon_database"
            )
                .addCallback(callback)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        @Provides
        @Singleton
        fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
            return appDatabase.taskDao()
        }

        @Provides
        @Singleton
        fun provideWorkBlockDao(appDatabase: AppDatabase): FocusBlockDao {
            return appDatabase.focusBlockDao()
        }

        @Provides
        @Singleton
        fun provideTaskRepository(
            taskDao: TaskDao,
            focusBlockDao: FocusBlockDao,
            taskContextDao: TaskContextDao,
            reminderManager: ReminderManager
        ): TaskRepository {
            return TaskRepository(taskDao, focusBlockDao, taskContextDao, reminderManager)
        }

        @Provides
        @Singleton
        fun provideTaskContextDao(appDatabase: AppDatabase): TaskContextDao {
            return appDatabase.taskContextDao()
        }

        @Provides
        @Singleton
        fun provideTaskContextRepository(
            taskContextDao: TaskContextDao,
            focusBlockDao: FocusBlockDao
        ): TaskContextRepository {
            return TaskContextRepository(taskContextDao, focusBlockDao)
        }

        @Provides
        @Singleton
        fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
            return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }
    }
}
