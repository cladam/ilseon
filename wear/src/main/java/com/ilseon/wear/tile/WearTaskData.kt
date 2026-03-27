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

        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_IS_URGENT = "is_urgent"
        const val KEY_DUE_TIME = "due_time"
        const val KEY_IS_RECORDING = "is_recording"

        // Message path for watch → phone action
        const val ACTION_TOGGLE_RECORDING = "/action/toggle-recording"
    }
}
