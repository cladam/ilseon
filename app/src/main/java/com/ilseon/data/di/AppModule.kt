package com.ilseon.data.di

import android.app.AlarmManager
import android.content.Context
import com.ilseon.AppDatabase
import com.ilseon.data.task.TaskContextDao
import com.ilseon.data.task.TaskContextRepository
import com.ilseon.data.task.TaskDao
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.task.FocusBlockDao
import com.ilseon.notifications.ReminderManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        // Use a Provider to break the circular dependency
        taskContextDaoProvider: Provider<TaskContextDao>
    ): AppDatabase {
        return AppDatabase.getDatabase(context, taskContextDaoProvider)
    }

    @Provides
    @Singleton
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }

    @Provides
    @Singleton
    fun provideWorkBlockDao(appDatabase: AppDatabase): FocusBlockDao {
        return appDatabase.FocusBlockDao()
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        focusBlockDao: FocusBlockDao,
        reminderManager: ReminderManager
    ): TaskRepository {
        return TaskRepository(taskDao, focusBlockDao, reminderManager)
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
