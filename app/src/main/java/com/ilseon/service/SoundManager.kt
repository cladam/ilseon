package com.ilseon.service

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import com.ilseon.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SoundManager {
    fun playWarningSound()
    fun playAlertSound()
    fun release()
}

@Singleton
class SoundManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SoundManager {

    private var mediaPlayer: MediaPlayer? = null

    override fun playWarningSound() {
        playSound(R.raw.mid_block_warning)
    }

    override fun playAlertSound() {
        playSound(R.raw.critical_alert)
    }

    private fun playSound(@RawRes soundResId: Int) {
        // Release any existing media player
        release()

        try {
            mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer?.setOnCompletionListener {
                release()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound", e)
            release()
        }
    }

    override fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
