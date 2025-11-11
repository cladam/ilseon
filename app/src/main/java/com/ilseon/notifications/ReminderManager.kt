package com.ilseon.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ilseon.data.task.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) {

    fun scheduleReminder(task: Task) {
        val pendingIntent = createPendingIntent(task)

        task.dueTime?.let { dueTime ->
            // Schedule a reminder for the due time
            if (dueTime > System.currentTimeMillis()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    // TODO: Handle case where exact alarms are not permitted.
                    // For now, we'll just log or skip. In a real app, you'd guide the user to settings.
                    return
                }
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dueTime,
                    pendingIntent
                )
            }
        }

        // Countdown reminder
        if (task.timerState == com.ilseon.data.task.TimerState.Running && task.remainingTimeInSeconds > 0) {
            val triggerAtMillis = System.currentTimeMillis() + task.remainingTimeInSeconds * 1000
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(task: Task) {
        val pendingIntent = createPendingIntent(task)
        alarmManager.cancel(pendingIntent)
    }

    private fun createPendingIntent(task: Task): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.ilseon.REMINDER"
            putExtra("EXTRA_TASK_ID", task.id.toString())
            putExtra("EXTRA_TASK_TITLE", task.title)
            putExtra("EXTRA_TASK_DESCRIPTION", task.description)
        }

        return PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
