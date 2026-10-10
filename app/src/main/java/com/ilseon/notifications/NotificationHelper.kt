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
        private const val CRITICAL_CHANNEL_ID = "ilseon_critical_decision_v3"
        private const val CRITICAL_CHANNEL_NAME = "Critical Decision"
        private const val CRITICAL_CHANNEL_DESCRIPTION = "High-priority alerts for starting or overdue tasks."

        // Tier 2
        private const val WARNING_CHANNEL_ID = "ilseon_pre_block_warning_v3"
        private const val WARNING_CHANNEL_NAME = "Pre-Block Warning"
        private const val WARNING_CHANNEL_DESCRIPTION = "Medium-priority warnings before a focus block ends."

        // Tier 1
        private const val ANCHOR_CHANNEL_ID = "ilseon_subtle_anchor_v3"
        private const val ANCHOR_CHANNEL_NAME = "Subtle Anchor"
        private const val ANCHOR_CHANNEL_DESCRIPTION = "Low-priority, subtle cues during a focus block."

        private const val NAGGING_CHANNEL_ID = "ilseon_nagging_v3"
        private const val NAGGING_CHANNEL_NAME = "Nagging"
        private const val NAGGING_CHANNEL_DESCRIPTION = "For repeated reminders that need attention."
        
        private const val SUCCESS_CHANNEL_ID = "ilseon_success_v3"
        private const val SUCCESS_CHANNEL_NAME = "Success"
        private const val SUCCESS_CHANNEL_DESCRIPTION = "For successful completion of tasks."

        private const val FOCUS_CHANNEL_ID = "ilseon_focus_v2"
        private const val FOCUS_CHANNEL_NAME = "Focus Session"
        private const val FOCUS_CHANNEL_DESCRIPTION = "Persistent notification for the active focus session"

        // Low-Sensory Audio Channels
        const val CHANNEL_HIGH_PRIORITY_AUDIO = "ilseon_tasks_high_audio_v1"
        private const val CHANNEL_HIGH_PRIORITY_AUDIO_NAME = "High-Priority Audio Cue"
        private const val CHANNEL_HIGH_PRIORITY_AUDIO_DESCRIPTION = "Low-sensory gentle wood tap for high-priority tasks."

        const val CHANNEL_URGENT_AUDIO = "ilseon_tasks_urgent_audio_v1"
        private const val CHANNEL_URGENT_AUDIO_NAME = "Urgent Audio Cue"
        private const val CHANNEL_URGENT_AUDIO_DESCRIPTION = "Low-sensory singing bowl chime for urgent tasks and incident follow-ups."
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val criticalChannel = NotificationChannel(
                CRITICAL_CHANNEL_ID,
                CRITICAL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CRITICAL_CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(true)
            }

            val warningChannel = NotificationChannel(
                WARNING_CHANNEL_ID,
                WARNING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = WARNING_CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(true)
            }

            val anchorChannel = NotificationChannel(
                ANCHOR_CHANNEL_ID,
                ANCHOR_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ANCHOR_CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(true)
            }

            val naggingChannel = NotificationChannel(
                NAGGING_CHANNEL_ID,
                NAGGING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = NAGGING_CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(true)
            }
            
            val successChannel = NotificationChannel(
                SUCCESS_CHANNEL_ID,
                SUCCESS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = SUCCESS_CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(true)
            }

            val focusChannel = NotificationChannel(
                FOCUS_CHANNEL_ID,
                FOCUS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = FOCUS_CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(false)
            }

            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val highSoundUri = android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.marimba_single}")
            val highPriorityAudioChannel = NotificationChannel(
                CHANNEL_HIGH_PRIORITY_AUDIO,
                CHANNEL_HIGH_PRIORITY_AUDIO_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_HIGH_PRIORITY_AUDIO_DESCRIPTION
                setSound(highSoundUri, audioAttributes)
                enableVibration(true)
            }

            val urgentSoundUri = android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.soft_singing_bowl_3}")
            val urgentAudioChannel = NotificationChannel(
                CHANNEL_URGENT_AUDIO,
                CHANNEL_URGENT_AUDIO_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_URGENT_AUDIO_DESCRIPTION
                setSound(urgentSoundUri, audioAttributes)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(
                    criticalChannel,
                    warningChannel,
                    anchorChannel,
                    naggingChannel,
                    successChannel,
                    focusChannel,
                    highPriorityAudioChannel,
                    urgentAudioChannel
                )
            )
        }
    }

    fun showHapticFeedback(tier: NotificationTier) {
        when (tier) {
            NotificationTier.CriticalDecision -> hapticManager.performAlert()
            NotificationTier.PreStartWarning -> hapticManager.performWarning()
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
        schedulingType: SchedulingType,
        isUrgent: Boolean = false,
        isUrgentAudioEnabled: Boolean = false,
        isHighPriorityAudioEnabled: Boolean = false
    ) {
        val channelId = when {
            isUrgent && isUrgentAudioEnabled -> CHANNEL_URGENT_AUDIO
            isHighPriorityAudioEnabled && (tier == NotificationTier.CriticalDecision || tier == NotificationTier.Nagging) -> CHANNEL_HIGH_PRIORITY_AUDIO
            else -> when (tier) {
                NotificationTier.CriticalDecision -> CRITICAL_CHANNEL_ID
                NotificationTier.PreStartWarning -> WARNING_CHANNEL_ID
                NotificationTier.PreBlockWarning -> WARNING_CHANNEL_ID
                NotificationTier.SubtleAnchor -> ANCHOR_CHANNEL_ID
                NotificationTier.Nagging -> NAGGING_CHANNEL_ID
                NotificationTier.Success -> SUCCESS_CHANNEL_ID
            }
        }

        val priority = when (tier) {
            NotificationTier.CriticalDecision -> NotificationCompat.PRIORITY_HIGH
            NotificationTier.PreStartWarning -> NotificationCompat.PRIORITY_DEFAULT
            NotificationTier.PreBlockWarning -> NotificationCompat.PRIORITY_DEFAULT
            NotificationTier.SubtleAnchor -> NotificationCompat.PRIORITY_LOW
            NotificationTier.Nagging -> NotificationCompat.PRIORITY_HIGH
            NotificationTier.Success -> NotificationCompat.PRIORITY_DEFAULT
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.ilseon.ACTION_SHOW_TASK"
            putExtra("EXTRA_TASK_ID", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

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
                val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = "com.ilseon.ACTION_COMPLETE_TASK"
                    putExtra("EXTRA_TASK_ID", taskId)
                }
                val completePendingIntent = PendingIntent.getBroadcast(
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
