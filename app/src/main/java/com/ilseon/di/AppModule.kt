package com.ilseon.di

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ilseon.AppDatabase
import com.ilseon.DatabaseCallback
import com.ilseon.data.idea.IdeaDao
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
                .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                .build()
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceDays TEXT")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN seriesId TEXT")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `Idea` (`id` TEXT NOT NULL, `content` TEXT, `createdAt` INTEGER NOT NULL, `isConverted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        @Provides
        @Singleton
        fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
            return appDatabase.taskDao()
        }
        
        @Provides
        @Singleton
        fun provideIdeaDao(appDatabase: AppDatabase): IdeaDao {
            return appDatabase.ideaDao()
        }

        @Provides
        @Singleton
        fun provideWorkBlockDao(appDatabase: AppDatabase): FocusBlockDao {
            return appDatabase.focusBlockDao()
        }

        @Provides
        @Singleton
        fun provideTaskRepository(
            @ApplicationContext context: Context,
            taskDao: TaskDao,
            focusBlockDao: FocusBlockDao,
            taskContextDao: TaskContextDao,
            reminderManager: ReminderManager
        ): TaskRepository {
            return TaskRepository(context, taskDao, focusBlockDao, taskContextDao, reminderManager)
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
