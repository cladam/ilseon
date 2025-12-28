package com.ilseon.service

import android.content.Context
import android.net.Uri
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.ilseon.data.task.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri


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

interface SpeechTranscriber {
    suspend fun transcribe(filePath: String): String?
    suspend fun extractTasksFromTranscript(transcript: String): String?
}

@Singleton
class GeminiSpeechTranscriber @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : SpeechTranscriber {

    override suspend fun transcribe(filePath: String): String? {
        val apiKey = settingsRepository.apiKey.first()
        Log.d("SpeechTranscriber", "API key length: ${apiKey.length}")
        if (apiKey.isEmpty()) {
            Log.e("SpeechTranscriber", "API key is empty!")
            return null
        }

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )

        val audioBytes = readFileToBytes(filePath) ?: return null

        val inputContent = content {
            text("Transcribe the following voice memo.")
            blob(
                mimeType = "audio/m4a",
                blob = audioBytes
            )
        }

        return try {
            val response = generativeModel.generateContent(inputContent)
            response.text?.let { Log.d("Transcription", it) }
            response.text
        } catch (e: Exception) {
            // Handle API errors
            e.printStackTrace()
            null
        }
    }

    override suspend fun extractTasksFromTranscript(transcript: String): String? {
        val apiKey = settingsRepository.apiKey.first()
        if (apiKey.isEmpty()) {
            Log.e("SpeechTranscriber", "API key is empty!")
            return null
        }

        val systemInstruction = """
        You are a task extraction engine. Analyze the user's transcript.
         Extract ALL mentioned tasks, ideas, action items, appointments, trips, events, and reminders.
         IMPORTANT: Keep task titles in the SAME LANGUAGE as the original transcript. Do not translate.
         For each item, provide:
         1. A "title".
         2. A "priority" (Low, Medium, or High).
         3. An "effort" level (Low, Medium, or High).
         Travel plans and scheduled activities should be extracted as separate tasks.
         Respond ONLY in structured JSON with this format: {"tasks": [{"title": "...", "priority": "...", "effort": "..."}]}
         If no tasks are found, return an empty list: {"tasks": []}
         """.trimIndent()


        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            },
            systemInstruction = content { text(systemInstruction) }
        )

        return try {
            val response = generativeModel.generateContent(content { text(transcript) })
            response.text?.also { Log.d("TaskExtraction", it) }
        } catch (e: Exception) {
            Log.e("TaskExtraction", "Error extracting tasks", e)
            e.printStackTrace()
            null
        }
    }

    private suspend fun readFileToBytes(filePath: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            if (filePath.startsWith("content://")) {
                val uri = filePath.toUri()
                context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes()
                }
            } else {
                File(filePath).readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
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
