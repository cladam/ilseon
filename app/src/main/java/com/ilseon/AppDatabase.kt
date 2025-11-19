package com.ilseon

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
import java.util.UUID


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
}
