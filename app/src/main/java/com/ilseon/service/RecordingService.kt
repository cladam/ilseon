package com.ilseon.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.IOException
import java.util.UUID

class RecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var activeOutputFile: File? = null
    private var startTime: Long = 0
    private var totalPausedMillis: Long = 0
    private var pauseStartTime: Long = 0

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun startRecording() {
        if (mediaRecorder != null) return

        val outputFile = createOutputFile() ?: return
        activeOutputFile = outputFile

        mediaRecorder = createMediaRecorder(outputFile).apply {
            try {
                prepare()
                start()
                startTime = System.currentTimeMillis()
                totalPausedMillis = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            } catch (e: IOException) {
                cancelRecording()
            }
        }
    }

    fun pauseRecording() {
        mediaRecorder?.pause()
        pauseStartTime = System.currentTimeMillis()
    }

    fun resumeRecording() {
        totalPausedMillis += (System.currentTimeMillis() - pauseStartTime)
        mediaRecorder?.resume()
    }

    fun stopRecording(): RecordingResult? {
        val duration = ((System.currentTimeMillis() - startTime - totalPausedMillis) / 1000).toInt()
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            release()
        }
        mediaRecorder = null
        stopForeground(STOP_FOREGROUND_REMOVE)

        val resultFile = activeOutputFile ?: return null
        activeOutputFile = null
        return RecordingResult(
            filePath = resultFile.absolutePath,
            durationSeconds = duration
        )
    }

    fun cancelRecording() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            release()
        }
        mediaRecorder = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        activeOutputFile?.delete()
        activeOutputFile = null
    }

    private fun createOutputFile(): File? {
        val outputDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
        } else {
            val dir = File(getExternalFilesDir(null), "Recordings")
            if (!dir.exists()) dir.mkdirs()
            dir
        }

        if (outputDir == null) {
            return null
        }

        return File(outputDir, "voice_memo_${UUID.randomUUID()}.m4a")
    }

    private fun createMediaRecorder(file: File): MediaRecorder {
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this)
        else @Suppress("DEPRECATION") MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Recording Voice Memo")
            .setContentText("Your thoughts are being captured...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Recording Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "RecordingServiceChannel"
    }
}