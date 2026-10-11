package com.ilseon.wear.tile

/**
 * Lightweight model representing the priority task on the watch.
 */
data class WearTaskData(
    val title: String,
    val description: String? = null,
    val isUrgent: Boolean = false,
    val dueTime: Long? = null
) {
    val isOverdue: Boolean
        get() = dueTime != null && dueTime < System.currentTimeMillis()

    companion object {
        // DataClient path constants
        const val PATH_PRIORITY_TASK = "/priority-task"
        const val PATH_RECORDING_STATE = "/recording-state"
        const val PATH_ROUTINE_STATE = "/routine-state"

        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_IS_URGENT = "is_urgent"
        const val KEY_DUE_TIME = "due_time"
        const val KEY_IS_RECORDING = "is_recording"

        // Routine keys
        const val KEY_ROUTINE_TITLE = "routine_title"
        const val KEY_STEP_TITLE = "step_title"
        const val KEY_STEP_INDEX = "step_index"
        const val KEY_TOTAL_STEPS = "total_steps"
        const val KEY_TARGET_MINS = "target_duration_mins"
        const val KEY_IS_ACTIVE = "is_active"
        const val KEY_IS_COMPLETE = "is_complete"

        // Message path for watch → phone action
        const val ACTION_TOGGLE_RECORDING = "/action/toggle-recording"
        const val ACTION_TRIGGER_FOLLOWUP = "/action/trigger-followup"
        const val ACTION_ROUTINE_NEXT = "/action/routine-next"
        const val ACTION_ROUTINE_SKIP = "/action/routine-skip"
        const val ACTION_ROUTINE_START = "/action/routine-start"
    }
}

/**
 * Lightweight model representing active routine state on the watch.
 */
data class WearRoutineData(
    val routineTitle: String,
    val stepTitle: String,
    val stepIndex: Int,
    val totalSteps: Int,
    val targetDurationMins: Int? = null,
    val isActive: Boolean = false,
    val isComplete: Boolean = false
)
