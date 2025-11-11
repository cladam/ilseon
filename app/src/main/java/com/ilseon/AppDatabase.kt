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


@Database(entities = [Task::class, TaskContext::class, FocusBlock::class], version = 8, exportSchema = false)
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
                            INSTANCE?.let {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = taskContextDaoProvider.get()
                                    dao.insertContext(TaskContext(name = "Work", displayOrder = 0))
                                    dao.insertContext(TaskContext(name = "Family", displayOrder = 1))
                                    dao.insertContext(TaskContext(name = "Personal", displayOrder = 2))
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