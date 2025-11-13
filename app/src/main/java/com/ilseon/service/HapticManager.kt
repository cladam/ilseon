package com.ilseon.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface HapticManager {
    fun performNudge()
    fun performWarning()
    fun performAlert()
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

    private val nudgePattern = longArrayOf(0, 50)
    private val warningPattern = longArrayOf(0, 100, 50, 100)
    private val alertPattern = longArrayOf(0, 150, 200, 150, 200, 150)

    // Tier 1
    override fun performNudge() {
        vibrate(nudgePattern)
    }

    // Tier 2
    override fun performWarning() {
        vibrate(warningPattern)
    }

    // Tier 3
    override fun performAlert() {
        vibrate(alertPattern)
    }

    private fun vibrate(pattern: LongArray) {
        if (vibrator.hasVibrator()) {
            Log.d("HapticManager", "Device has vibrator, attempting to vibrate.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } else {
            Log.d("HapticManager", "Device does not have a vibrator.")
        }
    }
}
