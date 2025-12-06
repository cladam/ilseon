package com.ilseon.service

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.ilseon.data.task.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Result object to hold the file, text, and duration
data class RecordingResult(
    val filePath: String,
    val transcription: String,
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
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : AudioHandler {

    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var activeOutputFile: File? = null
    private var latestTranscription: String = ""
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
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
            } catch (e: IOException) {
                cancelRecording()
                return
            }
        }

        startSpeechRecognizer()

        mediaRecorder?.start()
        startTime = System.currentTimeMillis()
        totalPausedMillis = 0
    }

    override fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
            speechRecognizer?.stopListening()
            pauseStartTime = System.currentTimeMillis()
        }
    }

    override suspend fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            totalPausedMillis += (System.currentTimeMillis() - pauseStartTime)
            mediaRecorder?.resume()
            startSpeechRecognizer()
        }
    }

    private suspend fun startSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val language = settingsRepository.sstLanguage.first()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle?) {
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                            appendTranscription(it)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                            appendTranscription(it)
                        }
                    }
                    override fun onError(error: Int) {
                        if (latestTranscription.isBlank()) {
                            latestTranscription = "(Transcription failed)"
                        }
                        // This might be called automatically.
                        // The UI should handle the stop.
                    }
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                startListening(intent)
            }
        }
    }

    private fun appendTranscription(newText: String) {
        if (!latestTranscription.endsWith(newText)) {
            latestTranscription = if (latestTranscription.isBlank()) newText else "$latestTranscription $newText"
        }
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

        // Stop and destroy the SpeechRecognizer
        speechRecognizer?.apply {
            stopListening()
            destroy()
        }
        speechRecognizer = null

        val resultFile = activeOutputFile ?: return null
        if (latestTranscription.isBlank()) {
            latestTranscription = "(Transcription failed)"
        }
        return RecordingResult(
            filePath = resultFile.absolutePath,
            transcription = latestTranscription.trim(),
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

        // Stop and destroy the SpeechRecognizer
        speechRecognizer?.destroy()
        speechRecognizer = null

        // Clean up the created file
        activeOutputFile?.delete()
        activeOutputFile = null

        // Reset state
        latestTranscription = ""
        startTime = 0
        totalPausedMillis = 0
    }
}
