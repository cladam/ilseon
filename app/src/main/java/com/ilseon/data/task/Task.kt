package com.ilseon.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// This should be in an external configuration file, testing with hardcoded values first
//enum class TaskContext {
//    Work,
//    Family,
//    Personal,
//    Shopping,
//    Health,
//    Tbdflow,
//    Choreo,
//    Ilseon,
//    Medi,
//    Blog
//}

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

    val contextId: UUID,

    val priority: TaskPriority,

    val dueTime: Long? = null, // Timestamp

    val durationMs: Long? = null, // Duration in milliseconds

    val location: String? = null,

    val isComplete: Boolean = false,

    val completedAt: Long? = null,

    val reminderType: ReminderType = ReminderType.Time,

    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_contexts")
data class TaskContext(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val displayOrder: Int = 0
)