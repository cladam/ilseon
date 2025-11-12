package com.ilseon.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
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
        // Tier 3
        private const val CRITICAL_CHANNEL_ID = "ilseon_critical_decision"
        private const val CRITICAL_CHANNEL_NAME = "Critical Decision"
        private const val CRITICAL_CHANNEL_DESCRIPTION = "High-priority alerts for starting or overdue tasks."

        // Tier 2
        private const val WARNING_CHANNEL_ID = "ilseon_pre_block_warning"
        private const val WARNING_CHANNEL_NAME = "Pre-Block Warning"
        private const val WARNING_CHANNEL_DESCRIPTION = "Medium-priority warnings before a focus block ends."

        // Tier 1
        private const val ANCHOR_CHANNEL_ID = "ilseon_subtle_anchor"
        private const val ANCHOR_CHANNEL_NAME = "Subtle Anchor"
        private const val ANCHOR_CHANNEL_DESCRIPTION = "Low-priority, subtle cues during a focus block."

        private const val FOCUS_CHANNEL_ID = "ilseon_focus"
        private const val FOCUS_CHANNEL_NAME = "Focus Session"
        private const val FOCUS_CHANNEL_DESCRIPTION = "Persistent notification for the active focus session"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val criticalChannel = NotificationChannel(
                CRITICAL_CHANNEL_ID,
                CRITICAL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CRITICAL_CHANNEL_DESCRIPTION
            }

            val warningChannel = NotificationChannel(
                WARNING_CHANNEL_ID,
                WARNING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = WARNING_CHANNEL_DESCRIPTION
            }

            val anchorChannel = NotificationChannel(
                ANCHOR_CHANNEL_ID,
                ANCHOR_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ANCHOR_CHANNEL_DESCRIPTION
            }

            val focusChannel = NotificationChannel(
                FOCUS_CHANNEL_ID,
                FOCUS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = FOCUS_CHANNEL_DESCRIPTION
            }

            notificationManager.createNotificationChannels(
                listOf(criticalChannel, warningChannel, anchorChannel, focusChannel)
            )
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showReminderNotification(
        taskId: String,
        title: String,
        description: String?,
        tier: NotificationTier
    ) {
        val channelId = when (tier) {
            NotificationTier.CriticalDecision -> CRITICAL_CHANNEL_ID
            NotificationTier.PreBlockWarning -> WARNING_CHANNEL_ID
            NotificationTier.SubtleAnchor -> ANCHOR_CHANNEL_ID
        }

        val priority = when (tier) {
            NotificationTier.CriticalDecision -> NotificationCompat.PRIORITY_HIGH
            NotificationTier.PreBlockWarning -> NotificationCompat.PRIORITY_DEFAULT
            NotificationTier.SubtleAnchor -> NotificationCompat.PRIORITY_LOW
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(priority)
            .setAutoCancel(true)

        notificationManager.notify(taskId.hashCode(), builder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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
