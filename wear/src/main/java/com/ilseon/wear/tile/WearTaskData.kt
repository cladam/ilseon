package com.ilseon.wear.tile

/**
 * Lightweight model representing the priority task on the watch.
 * This mirrors the essential fields from the phone app's Task entity.
 * Data will eventually arrive via DataClient from the phone.
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
        // DataClient path constants (for future phone ↔ watch sync)
        const val PATH_PRIORITY_TASK = "/priority-task"
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_IS_URGENT = "is_urgent"
        const val KEY_DUE_TIME = "due_time"

        // Message paths for watch → phone actions
        const val ACTION_NEW_TASK = "/action/new-task"
        const val ACTION_NEW_IDEA = "/action/new-idea"
        const val ACTION_NEW_VOICE_MEMO = "/action/new-voice-memo"
    }
}

