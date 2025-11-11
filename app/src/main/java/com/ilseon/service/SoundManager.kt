package com.ilseon.service

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import com.ilseon.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SoundManager {
    fun playWarningSound()
    fun playAlertSound()
}

@Singleton
class SoundManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SoundManager {

    override fun playWarningSound() {
        playSound(R.raw.mid_block_warning)
    }

    override fun playAlertSound() {
        playSound(R.raw.critical_alert)
    }

    private fun playSound(@RawRes soundResId: Int) {
        try {
            val mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            // Could fail if the resource doesn't exist.
            // Or if MediaPlayer fails for some other reason.
            e.printStackTrace()
        }
    }
}
