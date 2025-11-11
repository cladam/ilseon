package com.ilseon.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ilseon.MainActivity
import com.ilseon.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        private const val REMINDER_CHANNEL_ID = "ilseon_reminders"
        private const val REMINDER_CHANNEL_NAME = "Task Reminders"
        private const val REMINDER_CHANNEL_DESCRIPTION = "Notifications for task reminders"

        private const val FOCUS_CHANNEL_ID = "ilseon_focus"
        private const val FOCUS_CHANNEL_NAME = "Focus Session"
        private const val FOCUS_CHANNEL_DESCRIPTION = "Persistent notification for the active focus session"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = REMINDER_CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(reminderChannel)

            val focusChannel = NotificationChannel(
                FOCUS_CHANNEL_ID,
                FOCUS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = FOCUS_CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(focusChannel)
        }
    }

    fun showReminderNotification(taskId: String, title: String, description: String?) {
        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(taskId.hashCode(), builder.build())
    }

    fun showFocusNotification(taskName: String) {
        // Intent to open the app when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // TODO: Implement PendingIntents for "Extend Focus" and "Complete Task" actions
        val extendIntent = Intent() // Replace with your BroadcastReceiver or Service Intent
        val extendPendingIntent = PendingIntent.getBroadcast(context, 1, extendIntent, PendingIntent.FLAG_IMMUTABLE)

        val completeIntent = Intent() // Replace with your BroadcastReceiver or Service Intent
        val completePendingIntent = PendingIntent.getBroadcast(context, 2, completeIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, FOCUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focusing on: $taskName")
            .setContentText("Your focus session is in progress.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true) // Makes the notification persistent
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Extend Focus", extendPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Complete Task", completePendingIntent)


        // The ID for this notification should be unique and constant for the focus session
        notificationManager.notify(1, builder.build())
    }
}