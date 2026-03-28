package com.ilseon.wear

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.ilseon.service.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for messages from the Wear OS watch.
 * Handles /action/toggle-recording by directly toggling RecordingService.
 */
class WearActionListenerService : WearableListenerService() {

    companion object {
        private const val TAG = "WearActionListener"

        /**
         * Guard against duplicate delivery.  Both the manifest-declared
         * WearableListenerService *and* the programmatic MessageClient
         * listener can fire for the same message when the app process is
         * alive, which would start-then-immediately-stop a recording.
         */
        @Volatile
        private var lastToggleTimestamp: Long = 0
        private const val DEBOUNCE_MS = 2_000L          // ignore repeats within 2 s

        fun createMessageListener(context: Context): MessageClient.OnMessageReceivedListener {
            return MessageClient.OnMessageReceivedListener { messageEvent ->
                Log.d(TAG, "[Programmatic] Message received: ${messageEvent.path}")
                handleMessage(context, messageEvent)
            }
        }

        private fun handleMessage(context: Context, messageEvent: MessageEvent) {
            when (messageEvent.path) {
                "/action/toggle-recording" -> toggleRecording(context)
                else -> Log.w(TAG, "Unknown message path: ${messageEvent.path}")
            }
        }

        private fun toggleRecording(context: Context) {
            val now = System.currentTimeMillis()
            if (now - lastToggleTimestamp < DEBOUNCE_MS) {
                Log.d(TAG, "Ignoring duplicate toggle-recording (debounce)")
                return
            }
            lastToggleTimestamp = now

            Log.d(TAG, "Toggling recording from watch")

            val intent = Intent(context, RecordingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    val service = (binder as RecordingService.LocalBinder).getService()
                    // Toggle: same logic as the hardware media button
                    if (service.isRecording()) {
                        val result = service.stopRecording()
                        if (result != null) {
                            // Process the recording (save to voice memos)
                            service.processRecordingFromWear(result)
                        }
                        sendRecordingState(context, false)
                    } else {
                        service.startRecording()
                        sendRecordingState(context, true)
                    }
                    context.unbindService(this)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }

            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        private fun sendRecordingState(context: Context, isRecording: Boolean) {
            CoroutineScope(Dispatchers.IO).launch {
                WearDataSender.sendRecordingState(context, isRecording)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "[Service] Message received: ${messageEvent.path}")
        handleMessage(this, messageEvent)
    }
}
