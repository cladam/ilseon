package com.ilseon.wear.complication

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.ilseon.wear.tile.WearTaskDataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Transparent "trampoline" activity launched when the user taps the
 * Ilseon Voice Memo complication. It sends the toggle-recording message
 * to the phone and finishes immediately — the user never sees a UI.
 */
class ToggleRecordingActivity : Activity() {

    companion object {
        private const val TAG = "ToggleRecording"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Complication tapped — sending toggle-recording to phone")

        CoroutineScope(Dispatchers.IO).launch {
            WearTaskDataLoader.toggleRecording(this@ToggleRecordingActivity)
        }

        // Close right away — the phone handles the actual recording
        finish()
    }
}

