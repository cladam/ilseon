package com.ilseon.task

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// This should be in an external configuration file, testing with hardcoded values first
enum class TaskContext {
    Work,
    Family,
    Personal,
    Shopping,
    Health,
    Tbdflow,
    Choreo,
    Ilseon,
    Medi,
    Blog
}

enum class TaskPriority {
    High,
    Mid,
    Low
}

enum class ReminderType {
    Time,
    Location,
    Proximity
}

/**
 * This is the Task Entity (Data Model)
 * It defines the table structure for the Room database.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),

    val title: String,

    val context: TaskContext,

    val priority: TaskPriority,

    val dueTime: Long? = null, // Timestamp

    val isComplete: Boolean = false,

    val reminderType: ReminderType = ReminderType.Time,

    val createdAt: Long = System.currentTimeMillis()
)