package com.ilseon.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface HapticManager {
    fun performNudge()
    fun performWarning()
    fun performAlert()
    fun performSuccess()
    fun performNagging()
}

@Singleton
class HapticManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HapticManager {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val successPattern = longArrayOf(0, 120, 10, 50)
    private val nudgePattern = longArrayOf(0, 40)
    private val naggingPattern = longArrayOf(0, 100, 300, 100, 300, 100)
    private val warningPattern = longArrayOf(0, 80, 100, 80)
    private val alertPattern = longArrayOf(0, 50, 100, 50, 100, 50, 100, 50)

    // A very light tap, good for subtle feedback.
    override fun performNudge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrate(nudgePattern)
        }
    }

    override fun performWarning() {
        vibrate(warningPattern)
    }
    
    override fun performAlert() {
        vibrate(alertPattern)
    }

    override fun performSuccess() {
        vibrate(successPattern)
    }

    override fun performNagging() {
        vibrate(naggingPattern)
    }

    private fun vibrate(pattern: LongArray) {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrate(effect: VibrationEffect) {
        if (vibrator.hasVibrator()) {
            Log.d("HapticManager", "Device has vibrator, attempting to vibrate with effect.")
            vibrator.vibrate(effect)
        } else {
            Log.d("HapticManager", "Device does not have a vibrator.")
        }
    }
}
