package com.ilseon.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.ilseon.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationService {
    fun createNotificationChannel()
    fun sendTaskFinishedNotification(taskTitle: String)
}

@Singleton
class NotificationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationService {

    companion object {
        const val CHANNEL_ID = "task_alerts_channel"
        const val CHANNEL_NAME = "Task Alerts"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for when tasks are finished"
            // Sound is disabled by default (uses system default).
            // We can explicitly set it to null if needed, but we will leave it for now.
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun sendTaskFinishedNotification(taskTitle: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a real icon
            .setContentTitle("Task Finished")
            .setContentText("Your task '$taskTitle' has completed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
