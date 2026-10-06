package com.ilseon.wear.complication

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.ilseon.wear.tile.WearTaskData
import com.ilseon.wear.tile.WearTaskDataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Transparent "trampoline" activity launched when the user taps the
 * Quick Follow-Up complication. It immediately triggers tactile haptic feedback
 * on the wrist, sends /action/trigger-followup to the phone companion app,
 * and finishes immediately without displaying any UI.
 */
class QuickUrgentTaskActivity : Activity() {

    companion object {
        private const val TAG = "QuickUrgentTaskActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Quick Follow-Up complication tapped — triggering wrist haptic and sending message to phone")

        performWristHaptic()

        CoroutineScope(Dispatchers.IO).launch {
            WearTaskDataLoader.sendAction(this@QuickUrgentTaskActivity, WearTaskData.ACTION_TRIGGER_FOLLOWUP)
        }

        finish()
    }

    private fun performWristHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to perform wrist haptic feedback", e)
        }
    }
}
