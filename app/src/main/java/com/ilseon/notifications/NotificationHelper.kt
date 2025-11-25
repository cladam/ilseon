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
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.TimerState
import com.ilseon.service.HapticManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val hapticManager: HapticManager
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        // Haptic feedback is handled by HapticManager

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

        private const val NAGGING_CHANNEL_ID = "ilseon_nagging"
        private const val NAGGING_CHANNEL_NAME = "Nagging"
        private const val NAGGING_CHANNEL_DESCRIPTION = "For repeated reminders that need attention."
        
        private const val SUCCESS_CHANNEL_ID = "ilseon_success"
        private const val SUCCESS_CHANNEL_NAME = "Success"
        private const val SUCCESS_CHANNEL_DESCRIPTION = "For successful completion of tasks."

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

            val naggingChannel = NotificationChannel(
                NAGGING_CHANNEL_ID,
                NAGGING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = NAGGING_CHANNEL_DESCRIPTION
            }
            
            val successChannel = NotificationChannel(
                SUCCESS_CHANNEL_ID,
                SUCCESS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = SUCCESS_CHANNEL_DESCRIPTION
            }

            val focusChannel = NotificationChannel(
                FOCUS_CHANNEL_ID,
                FOCUS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = FOCUS_CHANNEL_DESCRIPTION
            }

            notificationManager.createNotificationChannels(
                listOf(criticalChannel, warningChannel, anchorChannel, naggingChannel, successChannel, focusChannel)
            )
        }
    }

    fun showHapticFeedback(tier: NotificationTier) {
        when (tier) {
            NotificationTier.CriticalDecision -> hapticManager.performAlert()
            NotificationTier.PreBlockWarning -> hapticManager.performWarning()
            NotificationTier.SubtleAnchor -> hapticManager.performNudge()
            NotificationTier.Nagging -> hapticManager.performNagging()
            NotificationTier.Success -> hapticManager.performSuccess()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showReminderNotification(
        taskId: String,
        title: String,
        description: String?,
        tier: NotificationTier,
        timerState: TimerState,
        schedulingType: SchedulingType
    ) {
        val channelId = when (tier) {
            NotificationTier.CriticalDecision -> CRITICAL_CHANNEL_ID
            NotificationTier.PreBlockWarning -> WARNING_CHANNEL_ID
            NotificationTier.SubtleAnchor -> ANCHOR_CHANNEL_ID
            NotificationTier.Nagging -> NAGGING_CHANNEL_ID
            NotificationTier.Success -> SUCCESS_CHANNEL_ID
        }

        val priority = when (tier) {
            NotificationTier.CriticalDecision -> NotificationCompat.PRIORITY_HIGH
            NotificationTier.PreBlockWarning -> NotificationCompat.PRIORITY_DEFAULT
            NotificationTier.SubtleAnchor -> NotificationCompat.PRIORITY_LOW
            NotificationTier.Nagging -> NotificationCompat.PRIORITY_HIGH
            NotificationTier.Success -> NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(priority)
            .setAutoCancel(true)

        if (tier == NotificationTier.CriticalDecision) {
            // Only show "Start" action for tasks that can be started
            if (timerState == TimerState.NotStarted && schedulingType != SchedulingType.None) {
                val startIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = "com.ilseon.ACTION_START_TASK"
                    putExtra("EXTRA_TASK_ID", taskId)
                    putExtra("EXTRA_NOTIFICATION_TIER", tier.name)
                }
                val startPendingIntent = PendingIntent.getBroadcast(
                    context,
                    (taskId + "_start").hashCode(),
                    startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_launcher_foreground, "Start", startPendingIntent)
            } else {
                // For all other critical notifications, show "Complete"
                val completeIntent = Intent(context, MainActivity::class.java).apply {
                    action = "com.ilseon.ACTION_SHOW_REFLECTION"
                    putExtra("EXTRA_TASK_ID", taskId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val completePendingIntent = PendingIntent.getActivity(
                    context,
                    (taskId + "_complete").hashCode(),
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_launcher_foreground, "Complete", completePendingIntent)
            }
        }

        notificationManager.notify(taskId.hashCode(), builder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showFocusNotification(taskName: String) {
        // Intent to open the app when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val extendIntent = Intent()
        val extendPendingIntent = PendingIntent.getBroadcast(context, 1, extendIntent, PendingIntent.FLAG_IMMUTABLE)

        val completeIntent = Intent()
        val completePendingIntent = PendingIntent.getBroadcast(context, 2, completeIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, FOCUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focusing on: $taskName")
            .setContentText("Your focus session is in progress.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Extend Focus", extendPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Complete Task", completePendingIntent)

        notificationManager.notify(1, builder.build())
    }
}
