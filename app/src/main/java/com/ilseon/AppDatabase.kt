package com.ilseon

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.ilseon.data.task.ReminderType
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextDao
import com.ilseon.data.task.TaskDao
import com.ilseon.data.task.TaskPriority
import java.util.UUID


@Database(entities = [Task::class, TaskContext::class], version = 2, exportSchema = false)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun taskContextDao(): TaskContextDao


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
    }

    companion object {
        // Singleton pattern to prevent multiple instances of the database
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ilseon_database"
                )
                    // This is not suitable for production.
                    // A proper migration strategy should be implemented.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}