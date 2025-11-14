package com.ilseon

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ilseon.data.task.ReminderType
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


@Database(entities = [Task::class, TaskContext::class, FocusBlock::class], version = 9, exportSchema = false)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun taskContextDao(): TaskContextDao

    abstract fun FocusBlockDao(): FocusBlockDao



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
                                    val focusBlockDao = database.FocusBlockDao()

                                    // Pre-populate with default contexts
                                    val workContextId = UUID.randomUUID()
                                    val workContext = TaskContext(id = workContextId, name = "Work", displayOrder = 0)
                                    taskContextDao.insertContext(workContext)
                                    focusBlockDao.insert(FocusBlock(
                                        id = UUID.randomUUID(),
                                        contextId = workContextId,
                                        startTime = "09:00",
                                        endTime = "16:00"
                                    ))

                                    val healthContext = TaskContext(id = UUID.randomUUID(), name = "Health", description = "Fix back pain", displayOrder = 0)
                                    taskContextDao.insertContext(healthContext)
                                    focusBlockDao.insert(FocusBlock(id = UUID.randomUUID(),
                                        contextId = healthContext.id,
                                        startTime = "23:00",
                                        endTime = "23:59"
                                    ))

                                    taskContextDao.insertContext(TaskContext(name = "Family", displayOrder = 1))
                                    taskContextDao.insertContext(TaskContext(name = "Personal", displayOrder = 2))

                                    // --- TEST DATA ---

                                    // Rule 1: Task as a Simple Note (no alarms)
                                    taskDao.insert(
                                        Task(
                                            title = "Buy milk",
                                            contextId = workContextId,
                                            priority = TaskPriority.Low,
                                            description = "This is a simple note and should not trigger any alarms."
                                        )
                                    )

                                    // Rule 2: Task with a Scheduled Start & End Time (multiple alarms)
                                    val now = System.currentTimeMillis()
                                    val startTime = now + 60 * 1000 // 1 minute from now
                                    val endTime = startTime + 5 * 60 * 1000 // 5-minute duration
                                    taskDao.insert(
                                        Task(
                                            title = "Prep presentation",
                                            contextId = workContextId,
                                            priority = TaskPriority.High,
                                            startTime = startTime,
                                            dueTime = endTime,
                                            endTime = endTime,
                                            description = "Scheduled to start in 1 minute, with a 5 minute duration."
                                        )
                                    )

                                    // Rule 3: Task with a Duration (alarms only after manual start)
                                    taskDao.insert(
                                        Task(
                                            title = "Read Chapter 3",
                                            contextId = workContextId,
                                            priority = TaskPriority.Mid,
                                            totalTimeInMinutes = 15,
                                            description = "15-min task. Timer must be started manually."
                                        )
                                    )
                                 }
                            }
                        }
                    })
                    // This is not suitable for production, at all....!
                    // TODO: Add proper migration strategy.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}