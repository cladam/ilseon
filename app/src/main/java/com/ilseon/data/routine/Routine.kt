package com.ilseon.data.routine

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.ilseon.data.EnergyLevel
import java.util.UUID

/**
 * Routine represents a reusable sequence of micro-steps.
 * It is not tied to a single calendar day instance and does not require manual daily resets.
 */
@Entity(
    tableName = "routines",
    indices = [
        Index(value = ["contextId"]),
        Index(value = ["isArchived"])
    ]
)
data class Routine(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String? = null,
    val contextId: UUID,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val endHour: Int? = null,
    val endMinute: Int? = null,
    val daysOfWeek: String? = null, // e.g. "MONDAY,TUESDAY,WEDNESDAY" or null for daily
    val resetCutoffHour: Int = 4,   // 04:00 AM cutoff for rolling day reset
    val displayOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * RoutineStep represents a single micro-action within a Routine.
 */
@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["routineId", "orderIndex"])
    ]
)
data class RoutineStep(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val routineId: UUID,
    val title: String,
    val orderIndex: Int = 0,
    val targetDurationMinutes: Int? = null, // Elastic duration pacing
    val energyLevel: EnergyLevel? = null
)

/**
 * RoutineRun tracks an execution session for a Routine on a given logical cycle date.
 * Steps completed or skipped are recorded here so the master routine steps never need manual resets.
 */
@Entity(
    tableName = "routine_runs",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["sessionDate"]),
        Index(value = ["routineId", "sessionDate"])
    ]
)
data class RoutineRun(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val routineId: UUID,
    val sessionDate: Long, // Start-of-logical-cycle timestamp (computed via cutoff hour)
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val currentStepIndex: Int = 0,
    val completedStepIds: List<String> = emptyList(),
    val skippedStepIds: List<String> = emptyList(),
    val isAbandoned: Boolean = false
)

/**
 * Aggregate data class holding a Routine and its steps in sorted order.
 */
data class RoutineWithSteps(
    @Embedded val routine: Routine,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val steps: List<RoutineStep>
) {
    val sortedSteps: List<RoutineStep>
        get() = steps.sortedBy { it.orderIndex }

    val totalEstimatedMinutes: Int
        get() = steps.sumOf { it.targetDurationMinutes ?: 0 }
}
