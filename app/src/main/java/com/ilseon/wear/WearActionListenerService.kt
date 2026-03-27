package com.ilseon.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.ilseon.ActionTrampolineActivity
import com.ilseon.R

/**
 * Listens for messages from the Wear OS watch and launches the
 * corresponding action on the phone via a heads-up notification.
 *
 * Two delivery paths ensure reliability:
 * 1. Manifest-declared WearableListenerService (works even if app is killed)
 * 2. Programmatic MessageClient.OnMessageReceivedListener (registered in IlseonApplication)
 */
class WearActionListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearActionListener"
        private const val CHANNEL_ID = "ilseon_wear_action"
        private const val NOTIFICATION_ID = 9001

        /**
         * Creates a programmatic listener that can be registered via
         * MessageClient.addListener(). Uses the same notification logic
         * as the manifest-declared service.
         */
        fun createMessageListener(context: Context): MessageClient.OnMessageReceivedListener {
            return MessageClient.OnMessageReceivedListener { messageEvent ->
                Log.d(TAG, "[Programmatic] Message received: ${messageEvent.path}")
                handleMessage(context, messageEvent)
            }
        }

        private fun handleMessage(context: Context, messageEvent: MessageEvent) {
            val (intentAction, label) = when (messageEvent.path) {
                "/action/new-task"       -> "com.ilseon.action.NEW_TASK" to "New Task"
                "/action/new-idea"       -> "com.ilseon.action.NEW_IDEA" to "New Idea"
                "/action/new-voice-memo" -> "com.ilseon.action.NEW_VOICE_MEMO" to "New Voice Memo"
                else -> {
                    Log.w(TAG, "Unknown message path: ${messageEvent.path}")
                    return
                }
            }

            ensureNotificationChannel(context)

            val intent = Intent(context, ActionTrampolineActivity::class.java).apply {
                action = intentAction
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                intentAction.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Ilseon")
                .setContentText(label)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Posted notification for: $intentAction")
        }

        private fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Watch Shortcuts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Actions triggered from your watch"
                    setSound(null, null)
                }
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "[Service] Message received: ${messageEvent.path}")
        handleMessage(this, messageEvent)
    }
}
