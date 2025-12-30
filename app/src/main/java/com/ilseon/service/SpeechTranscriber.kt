package com.ilseon.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
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
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import kotlin.invoke
import kotlin.text.compareTo


// Result object to hold the file, text, and duration
data class RecordingResult(
    val filePath: String,
    val durationSeconds: Int
)

interface AudioHandler {
    fun isRecording(): Boolean
    fun startRecording(onStarted: () -> Unit = {})
    fun stopRecording(): RecordingResult?
    fun cancelRecording()
    fun pauseRecording()
    fun resumeRecording()
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

    private var recordingService: RecordingService? = null
    private var isBound = false
    private var pendingStartRecording = false
    private var pendingOnStarted: (() -> Unit)? = null
    private var isCurrentlyRecording = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d("AudioHandler", "Service connected")
            val binder = service as RecordingService.LocalBinder
            recordingService = binder.getService()
            isBound = true
            if (pendingStartRecording) {
                recordingService?.startRecording()
                pendingOnStarted?.invoke()
                pendingOnStarted = null
                pendingStartRecording = false
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d("AudioHandler", "Service disconnected")
            isBound = false
            recordingService = null
        }
    }

    private fun bindToService() {
        val intent = Intent(context, RecordingService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    override fun isRecording(): Boolean = isCurrentlyRecording

    override fun startRecording(onStarted: () -> Unit) {
        Log.d("AudioHandler", "startRecording called, isBound=$isBound")
        if (isBound) {
            recordingService?.startRecording()
            isCurrentlyRecording = true
            onStarted()
        } else {
            pendingStartRecording = true
            pendingOnStarted = {
                isCurrentlyRecording = true
                onStarted()
            }
            //bindToService()
            val intent = Intent(context, RecordingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun stopRecording(): RecordingResult? {
        Log.d("AudioHandler", "stopRecording called, isBound=$isBound, service=$recordingService")
        isCurrentlyRecording = false
        return if (isBound) {
            val result = recordingService?.stopRecording()
            Log.d("AudioHandler", "stopRecording result: $result")
            result
        } else {
            Log.w("AudioHandler", "stopRecording called but service not bound!")
            null
        }
    }

    override fun cancelRecording() {
        isCurrentlyRecording = false
        if (isBound) {
            recordingService?.cancelRecording()
        }
    }

    override fun pauseRecording() {
        if (isBound) {
            recordingService?.pauseRecording()
        }
    }

    override fun resumeRecording() {
        if (isBound) {
            recordingService?.resumeRecording()
        }
    }
}

