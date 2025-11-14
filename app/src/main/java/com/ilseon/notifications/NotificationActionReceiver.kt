package com.ilseon.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.task.TimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val taskIdString = intent.getStringExtra("EXTRA_TASK_ID") ?: return
        val taskId = UUID.fromString(taskIdString)

        CoroutineScope(Dispatchers.IO).launch {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                when (intent.action) {
                    "com.ilseon.ACTION_COMPLETE_TASK" -> {
                        taskRepository.updateTask(task.copy(isComplete = true))
                        // Dismiss the notification
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        notificationManager.cancel(taskIdString.hashCode())
                    }

                    "com.ilseon.ACTION_START_TASK" -> {
                        val updatedTask =
                            task.copy(timerState = TimerState.Running, timerStartTime = System.currentTimeMillis())
                        taskRepository.updateTask(updatedTask)

                        val tierName = intent.getStringExtra("EXTRA_NOTIFICATION_TIER")
                        val tier = tierName?.let { NotificationTier.valueOf(it) } ?: NotificationTier.CriticalDecision

                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationHelper.showReminderNotification(
                                updatedTask.id.toString(),
                                updatedTask.title,
                                updatedTask.description,
                                tier,
                                updatedTask.timerState
                            )
                        }
                    }
                }
            }
        }
    }
}
