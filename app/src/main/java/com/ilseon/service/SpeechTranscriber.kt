package com.ilseon.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Result object to hold the file, text, and duration
data class RecordingResult(
    val filePath: String,
    val durationSeconds: Int
)

interface AudioHandler {
    suspend fun startRecording()
    fun stopRecording(): RecordingResult?
    fun cancelRecording()
    fun pauseRecording()
    suspend fun resumeRecording()
}

@Singleton
class AudioHandlerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioHandler {

    private var mediaRecorder: MediaRecorder? = null
    private var activeOutputFile: File? = null
    private var startTime: Long = 0
    private var totalPausedMillis: Long = 0
    private var pauseStartTime: Long = 0

    override suspend fun startRecording() {
        if (mediaRecorder != null) return // Already recording

        val outputDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
        } else {
            val dir = File(context.getExternalFilesDir(null), "Recordings")
            if (!dir.exists()) dir.mkdirs()
            dir
        }
        if (outputDir == null) return

        val outputFile = File(outputDir, "voice_memo_${UUID.randomUUID()}.m4a")
        activeOutputFile = outputFile

        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC_ELD)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
            } catch (e: IOException) {
                cancelRecording()
                return
            }
        }

        mediaRecorder?.start()
        startTime = System.currentTimeMillis()
        totalPausedMillis = 0
    }

    override fun pauseRecording() {
        mediaRecorder?.pause()
        pauseStartTime = System.currentTimeMillis()
    }

    override suspend fun resumeRecording() {
        totalPausedMillis += (System.currentTimeMillis() - pauseStartTime)
        mediaRecorder?.resume()
    }

    override fun stopRecording(): RecordingResult? {
        val duration = ((System.currentTimeMillis() - startTime - totalPausedMillis) / 1000).toInt()

        // Gracefully stop and release the MediaRecorder
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                // This can happen if the recorder is already stopped.
                e.printStackTrace()
            }
            try {
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null

        val resultFile = activeOutputFile ?: return null
        return RecordingResult(
            filePath = resultFile.absolutePath,
            durationSeconds = duration
        )
    }

    override fun cancelRecording() {
        // Gracefully stop and release the MediaRecorder
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            try {
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null

        // Clean up the created file
        activeOutputFile?.delete()
        activeOutputFile = null

        // Reset state
        startTime = 0
        totalPausedMillis = 0
    }
}