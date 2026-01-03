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

    private fun performVibration(effectProvider: () -> VibrationEffect, legacyPattern: LongArray) {
        if (!vibrator.hasVibrator()) {
            Log.d("HapticManager", "Device does not have a vibrator.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("HapticManager", "Device has vibrator, attempting to vibrate with effect.")
            vibrator.vibrate(effectProvider())
        } else {
            Log.d("HapticManager", "Device has vibrator, attempting to vibrate with legacy pattern.")
            @Suppress("DEPRECATION")
            vibrator.vibrate(legacyPattern, -1)
        }
    }

    override fun performNudge() {
        Log.d("HapticManager", "performNudge() called")
        performVibration(
            effectProvider = { VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE) },
            legacyPattern = nudgePattern
        )
    }

    override fun performWarning() {
        performVibration(
            effectProvider = { VibrationEffect.createWaveform(warningPattern, -1) },
            legacyPattern = warningPattern
        )
    }

    override fun performAlert() {
        performVibration(
            effectProvider = { VibrationEffect.createWaveform(alertPattern, -1) },
            legacyPattern = alertPattern
        )
    }

    override fun performSuccess() {
        performVibration(
            effectProvider = { VibrationEffect.createWaveform(successPattern, -1) },
            legacyPattern = successPattern
        )
    }

    override fun performNagging() {
        performVibration(
            effectProvider = { VibrationEffect.createWaveform(naggingPattern, -1) },
            legacyPattern = naggingPattern
        )
    }
}
