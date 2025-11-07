package com.ilseon.data.local

import androidx.room.TypeConverter
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.ReminderType
import java.util.UUID

class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID): String {
        return uuid.toString()
    }

    @TypeConverter
    fun toUUID(uuid: String): UUID {
        return UUID.fromString(uuid)
    }

    @TypeConverter
    fun fromTaskContext(value: TaskContext) = value.name

    @TypeConverter
    fun toTaskContext(value: String) = enumValueOf<TaskContext>(value)

    @TypeConverter
    fun fromTaskPriority(value: TaskPriority) = value.name

    @TypeConverter
    fun toTaskPriority(value: String) = enumValueOf<TaskPriority>(value)
    
    @TypeConverter
    fun fromReminderType(value: ReminderType) = value.name

    @TypeConverter
    fun toReminderType(value: String) = enumValueOf<ReminderType>(value)
}
