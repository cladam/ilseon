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
import com.ilseon.data.userstatus.UserStatusDao
import com.ilseon.data.userstatus.UserStatusRepository
import com.ilseon.data.voicememo.VoiceMemoDao
import com.ilseon.data.voicememo.VoiceMemoRepository
import com.ilseon.notifications.IReminderManager
import com.ilseon.notifications.ReminderManager
import com.ilseon.service.AudioHandler
import com.ilseon.service.AudioHandlerImpl
import com.ilseon.service.GeminiSpeechTranscriber
import com.ilseon.service.HapticManager
import com.ilseon.service.HapticManagerImpl
import com.ilseon.service.SpeechTranscriber
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

    @Binds
    @Singleton
    abstract fun bindReminderManager(impl: ReminderManager): IReminderManager

    @Binds
    @Singleton
    abstract fun bindAudioHandler(impl: AudioHandlerImpl): AudioHandler

    @Binds
    @Singleton
    abstract fun bindSpeechTranscriber(impl: GeminiSpeechTranscriber): SpeechTranscriber

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
                .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25)
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

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isUrgent INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `voice_memos` (`id` TEXT NOT NULL, `transcription` TEXT NOT NULL, `filePath` TEXT NOT NULL, `durationSeconds` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voice_memos ADD COLUMN title TEXT NOT NULL DEFAULT 'Voice Memo'")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create a new table with the desired schema (without the transcription column)
                db.execSQL("CREATE TABLE `voice_memos_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `filePath` TEXT NOT NULL, `durationSeconds` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                // Copy the data from the old table to the new table
                db.execSQL("INSERT INTO `voice_memos_new` (`id`, `title`, `filePath`, `durationSeconds`, `timestamp`) SELECT `id`, `title`, `filePath`, `durationSeconds`, `timestamp` FROM `voice_memos`")

                // Remove the old table
                db.execSQL("DROP TABLE `voice_memos`")

                // Rename the new table to the original table name
                db.execSQL("ALTER TABLE `voice_memos_new` RENAME TO `voice_memos`")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN parentId TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Idea ADD COLUMN isReference INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE Idea ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_21_22: Migration = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN energyLevel TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN actualEnergyLevel TEXT")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `UserStatus` (`userId` TEXT NOT NULL, `currentEnergy` TEXT NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`userId`))")
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE idea ADD COLUMN weight INTEGER NOT NULL DEFAULT 0")
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
        fun provideVoiceMemoDao(appDatabase: AppDatabase): VoiceMemoDao {
            return appDatabase.voiceMemoDao()
        }

        @Provides
        @Singleton
        fun provideUserStatusDao(appDatabase: AppDatabase): UserStatusDao {
            return appDatabase.userStatusDao()
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
            reminderManager: IReminderManager,
            userStatusRepository: UserStatusRepository
        ): TaskRepository {
            return TaskRepository(context, taskDao, focusBlockDao, taskContextDao, reminderManager, userStatusRepository)
        }

        @Provides
        @Singleton
        fun provideVoiceMemoRepository(voiceMemoDao: VoiceMemoDao): VoiceMemoRepository {
            return VoiceMemoRepository(voiceMemoDao)
        }

        @Provides
        @Singleton
        fun provideUserStatusRepository(userStatusDao: UserStatusDao): UserStatusRepository {
            return UserStatusRepository(userStatusDao)
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