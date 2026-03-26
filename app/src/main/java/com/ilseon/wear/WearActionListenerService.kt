package com.ilseon.wear

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.ilseon.ActionTrampolineActivity

/**
 * Listens for messages from the Wear OS watch and launches the
 * corresponding action on the phone via ActionTrampolineActivity.
 *
 * Message paths map directly to the existing intent actions:
 *   /action/new-task       → com.ilseon.action.NEW_TASK
 *   /action/new-idea       → com.ilseon.action.NEW_IDEA
 *   /action/new-voice-memo → com.ilseon.action.NEW_VOICE_MEMO
 */
class WearActionListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearActionListener"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received: ${messageEvent.path}")

        val intentAction = when (messageEvent.path) {
            "/action/new-task" -> "com.ilseon.action.NEW_TASK"
            "/action/new-idea" -> "com.ilseon.action.NEW_IDEA"
            "/action/new-voice-memo" -> "com.ilseon.action.NEW_VOICE_MEMO"
            else -> {
                Log.w(TAG, "Unknown message path: ${messageEvent.path}")
                return
            }
        }

        val intent = Intent(this, ActionTrampolineActivity::class.java).apply {
            action = intentAction
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        Log.d(TAG, "Launched action: $intentAction")
    }
}

