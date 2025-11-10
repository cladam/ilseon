package com.ilseon.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

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

enum class TimerState {
    NotStarted,
    Running,
    Paused,
    Finished
}

enum class SchedulingType {
    None,
    TimeBlock,
    Duration
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
    val description: String? = null,
    val priority: TaskPriority,
    val dueTime: Long? = null, // Timestamp for when the task is due
    val startTime: Long? = null, // Start of the time block
    val endTime: Long? = null, // End of the time block
    val totalTimeInMinutes: Int? = null, // Original planned duration
    var remainingTimeInSeconds: Long = totalTimeInMinutes?.times(60L) ?: 0,
    var timerState: TimerState = TimerState.NotStarted,
    val timerStartTime: Long? = null, // Actual timestamp when the timer was started
    val isCurrentPriority: Boolean = false,
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