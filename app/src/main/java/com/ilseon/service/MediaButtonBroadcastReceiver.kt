package com.ilseon.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.KeyEvent

/**
 * Receives MEDIA_BUTTON broadcasts from the system and forwards them to RecordingService.
 * Only forwards if the media button trigger setting is enabled — otherwise does nothing
 * so other apps (e.g. Spotify) can handle the event.
 */
class MediaButtonBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return

        // Check if the user has enabled the media button trigger
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("media_button_trigger_enabled", false)
        if (!enabled) {
            Log.d("MediaButtonReceiver", "Media button trigger disabled, ignoring")
            return
        }

        Log.d("MediaButtonReceiver", "onReceive: forwarding to RecordingService")

        val serviceIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setClass(context, RecordingService::class.java)
            intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)?.let {
                putExtra(Intent.EXTRA_KEY_EVENT, it)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("MediaButtonReceiver", "Failed to start RecordingService", e)
        }
    }
}
