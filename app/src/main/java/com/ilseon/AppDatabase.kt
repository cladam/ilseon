package com.ilseon

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ilseon.data.task.ReminderType
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextDao
import com.ilseon.data.task.TaskDao
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TimerState
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.FocusBlockDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Provider


@Database(entities = [Task::class, TaskContext::class, FocusBlock::class], version = 11, exportSchema = false)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun taskContextDao(): TaskContextDao

    abstract fun focusBlockDao(): FocusBlockDao

    /**
     * TypeConverters to tell Room how to store Enum classes in the database.
     */
    class Converters {
        @TypeConverter
        fun fromUUID(uuid: UUID?): String? {
            return uuid?.toString()
        }

        @TypeConverter
        fun toUUID(uuid: String?): UUID? {
            return uuid?.let { UUID.fromString(it) }
        }

        @TypeConverter
        fun fromTaskPriority(value: TaskPriority): String = value.name

        @TypeConverter
        fun toTaskPriority(value: String): TaskPriority = TaskPriority.valueOf(value)

        @TypeConverter
        fun fromReminderType(value: ReminderType): String = value.name

        @TypeConverter
        fun toReminderType(value: String): ReminderType = ReminderType.valueOf(value)

        @TypeConverter
        fun fromTimerState(value: TimerState): String = value.name

        @TypeConverter
        fun toTimerState(value: String): TimerState = TimerState.valueOf(value)

        @TypeConverter
        fun fromString(value: String?): List<Int> {
            return value?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        }

        @TypeConverter
        fun fromList(list: List<Int>?): String {
            return list?.joinToString(",") ?: ""
        }
    }

    companion object {
        // Singleton pattern to prevent multiple instances of the database
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, taskContextDaoProvider: Provider<TaskContextDao>): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ilseon_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val taskContextDao = database.taskContextDao()
                                    val taskDao = database.taskDao()
                                    val focusBlockDao = database.focusBlockDao()

                                    // Pre-populate with ilseon contexts and tasks
                                    val ilseonContextId = UUID.randomUUID()
                                    val ilseonContext = TaskContext(id = ilseonContextId, name = "Ilseon", description = "Ilseon Context", displayOrder = 4)
                                    taskContextDao.insertContext(ilseonContext)
                                    taskDao.insert(
                                        Task(
                                            title = "Welcome to Ilseon!",
                                            contextId = ilseonContextId,
                                            priority = TaskPriority.High,
                                            description = "This is a regular task, click the checkbox to complete it. If you add a reflection of how the task went it will show up in Notes."
                                        )
                                    )
                                    taskDao.insert(
                                        Task(
                                            title = "Create a new context",
                                            contextId = ilseonContextId,
                                            priority = TaskPriority.High,
                                            description = "Contexts are powerful tools to group your tasks. You can create contexts for work, home, or any project you're working on. Just go to the Contexts screen and add a context, with or without a <b>Focus Block</b>."
                                        )
                                    )
                                    taskDao.insert(
                                        Task(
                                            title = "Schedule a task with a time block",
                                            contextId = ilseonContextId,
                                            priority = TaskPriority.Medium,
                                            description = "This task is scheduled for a specific time block.",
                                            schedulingType = SchedulingType.TimeBlock,
                                            startTime = System.currentTimeMillis() + 3600000, // 1 hour from now
                                            endTime = System.currentTimeMillis() + 7200000 // 2 hours from now
                                        )
                                    )
                                    taskDao.insert(
                                        Task(
                                            title = "Schedule a task with a duration",
                                            contextId = ilseonContextId,
                                            priority = TaskPriority.Medium,
                                            description = "This task has a duration of 1 hour.",
                                            schedulingType = SchedulingType.Duration,
                                            totalTimeInMinutes = 25
                                        )
                                    )
                                    taskDao.insert(
                                        Task(
                                            title = "Check your patterns",
                                            contextId = ilseonContextId,
                                            priority = TaskPriority.Low,
                                            description = "Make sure to check the Analytics screen to see your patterns and how you're spending your time."
                                        )
                                    )
                                 }
                            }
                        }
                    })
                    // This is not suitable for production, at all....!
                    // TODO: Add proper migration strategy.
                    .fallbackToDestructiveMigration()
                    //.addMigrations(MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Since there were no schema changes between version 9 and 10, there is no need to execute any SQL queries here.
                // If you were to add a new column to the Task table, you would do it like this:
                // db.execSQL("ALTER TABLE task ADD COLUMN new_column INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // I forgot to do the migration for the new column.
                // db.execSQL("ALTER TABLE task ADD COLUMN schedulingType INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}