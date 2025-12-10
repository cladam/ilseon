package com.ilseon.notifications

import com.ilseon.data.task.Task

interface IReminderManager {
    fun rescheduleReminders(task: Task)
    fun scheduleTimedTaskReminders(task: Task)
    fun scheduleDurationTaskReminders(task: Task)
    fun cancelAllReminders(task: Task)
}
