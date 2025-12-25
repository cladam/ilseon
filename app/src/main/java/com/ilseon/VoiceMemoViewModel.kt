package com.ilseon

import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.idea.IdeaRepository
import com.ilseon.data.task.ExtractedTasks
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.data.voicememo.VoiceMemoRepository
import com.ilseon.service.SpeechTranscriber
import com.ilseon.ui.mapEffortToEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class VoiceMemoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceMemoRepository: VoiceMemoRepository,
    private val speechTranscriber: SpeechTranscriber,
    private val ideaRepository: IdeaRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null
    private var progressTrackerJob: Job? = null

    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId = _currentlyPlayingId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress = _playbackProgress.asStateFlow()

    private val _transcribingMemoId = MutableStateFlow<String?>(null)
    val transcribingMemoId = _transcribingMemoId.asStateFlow()

    private val _transcriptionResult = MutableStateFlow<UUID?>(null)
    val transcriptionResult = _transcriptionResult.asStateFlow()

    private val _extractedTasks = MutableStateFlow<ExtractedTasks?>(null)
    val extractedTasks = _extractedTasks.asStateFlow()

    val voiceMemos = voiceMemoRepository.getVoiceMemos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isApiKeySet = settingsRepository.apiKey
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun transcribeMemo(memo: VoiceMemo) {
        if (_transcribingMemoId.value != null) return // Already transcribing

        Log.d("VoiceMemoVM", "transcribeMemo called for: ${memo.id}")
        viewModelScope.launch {
            _transcribingMemoId.value = memo.id
            var newIdeaId: UUID? = null // Hold the idea ID
            try {
                // 1. Transcribe
                val transcription = speechTranscriber.transcribe(memo.filePath)
                if (transcription.isNullOrBlank()) {
                    Log.w("VoiceMemoVM", "Transcription was null or blank")
                    return@launch
                }

                // 2. Create the Idea/Note immediately
                val deeplink = "[Play Recording](ilseon://play-voice-memo/${memo.id})"
                val fullContent = "$transcription\n\n$deeplink"
                newIdeaId = ideaRepository.insertIdea(
                    content = fullContent,
                    isReference = true
                )
                Log.d("VoiceMemoVM", "Idea inserted successfully with ID: $newIdeaId")

                // 3. Attempt to extract tasks
                val jsonResponse = try {
                    speechTranscriber.extractTasksFromTranscript(transcription)
                } catch (e: Exception) {
                    Log.e("VoiceMemoVM", "Task extraction failed, proceeding without tasks", e)
                    null // Ensure failure here doesn't crash the flow
                }

                if (!jsonResponse.isNullOrBlank()) {
                    val format = Json { isLenient = true }
                    val extractedTasks = format.decodeFromString<ExtractedTasks>(jsonResponse)
                    _extractedTasks.value = extractedTasks
                } else {
                    // If no tasks, navigate directly to the created note
                    _transcriptionResult.value = newIdeaId
                }

            } catch (e: Exception) {
                Log.e("VoiceMemoVM", "Transcription process failed", e)
                // If transcription itself fails, we might not have an ideaId to navigate to.
                // We could show a Snackbar or some other error feedback here.
            } finally {
                _transcribingMemoId.value = null
            }
        }
    }


    fun saveExtractedTasks(tasks: ExtractedTasks) {
        viewModelScope.launch {
            val importedContext = taskRepository.getOrCreateImportedContext()
            tasks.tasks.forEach { taskInfo ->
                val newTask = Task(
                    title = taskInfo.title,
                    energyLevel = mapEffortToEnum(taskInfo.effort),
                    contextId = importedContext.id,
                    priority = TaskPriority.Medium, // Default priority
                    createdAt = System.currentTimeMillis()
                )
                taskRepository.insertTask(newTask)
            }
            _extractedTasks.value = null // Reset after saving
        }
    }

    fun dismissExtractedTasks() {
        _extractedTasks.value = null
    }

    fun onPlayPause(memoId: String) {
        viewModelScope.launch {
            val memo = voiceMemoRepository.getVoiceMemo(memoId)
            if (memo != null) {
                if (_currentlyPlayingId.value == memo.id) {
                    stopPlayback()
                } else {
                    startPlayback(memo)
                }
            }
        }
    }

    fun resetTranscriptionResult() {
        _transcriptionResult.value = null
    }

    fun onPlayPause(memo: VoiceMemo) {
        if (_currentlyPlayingId.value == memo.id) {
            stopPlayback()
        } else {
            startPlayback(memo)
        }
    }

    private fun startPlayback(memo: VoiceMemo) {
        stopPlayback() // Stop any previous playback
        mediaPlayer = MediaPlayer().apply {
            try {
                val uri = if (memo.filePath.startsWith("content://")) {
                    memo.filePath.toUri()
                } else {
                    Uri.fromFile(File(memo.filePath))
                }
                setDataSource(context, uri)
                prepareAsync()
                setOnPreparedListener { player ->
                    player.start()
                    _currentlyPlayingId.value = memo.id
                    startProgressTracker()
                }
                setOnCompletionListener {
                    stopPlayback()
                }
            } catch (e: IOException) {
                stopPlayback()
            }
        }
    }

    private fun stopPlayback() {
        progressTrackerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        _currentlyPlayingId.value = null
        _playbackProgress.value = 0f
    }

    fun seekTo(progress: Float) {
        mediaPlayer?.let {
            val newPosition = (it.duration * progress).toInt()
            it.seekTo(newPosition)
            _playbackProgress.value = progress
        }
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = viewModelScope.launch {
            while (_currentlyPlayingId.value != null) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _playbackProgress.value = it.currentPosition.toFloat() / it.duration
                    }
                }
                delay(50) // Update progress roughly 20 times per second
            }
        }
    }

    fun saveVoiceMemo(filePath: String, durationSeconds: Int) {
        viewModelScope.launch {
            val title = "Voice Memo"

            val finalUri = saveRecordingToMediaStore(filePath, title)

            if (finalUri != null) {
                val voiceMemo = VoiceMemo(
                    title = title,
                    filePath = finalUri.toString(),
                    durationSeconds = durationSeconds
                )
                voiceMemoRepository.insert(voiceMemo)
            }
        }
    }

    private suspend fun saveRecordingToMediaStore(tempFilePath: String, title: String): Uri? = withContext(Dispatchers.IO) {
        val sanitizedTitle = title
            .replace(Regex("[^a-zA-Z0-9.-]+"), "_")
            .trim('_')
            .let { it.ifBlank { "voicememo" } }
            .take(100)
        val displayName = "${sanitizedTitle}_${System.currentTimeMillis()}.m4a"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recordingsFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Environment.DIRECTORY_RECORDINGS
                } else {
                    "Recordings"
                }
                put(MediaStore.MediaColumns.RELATIVE_PATH, recordingsFolder + File.separator + "ilseon")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                val tempFile = File(tempFilePath)
                resolver.openOutputStream(it).use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream!!)
                    }
                }
                tempFile.delete() // Clean up temp file
                return@withContext it
            } catch (e: IOException) {
                resolver.delete(it, null, null)
                e.printStackTrace()
            }
        }
        return@withContext null
    }


    fun updateVoiceMemoTitle(memo: VoiceMemo, newTitle: String) {
        viewModelScope.launch {
            if (memo.filePath.startsWith("content://")) {
                val uri = memo.filePath.toUri()
                val currentDisplayName = getDisplayName(uri)
                val extension = currentDisplayName?.substringAfterLast(".", "") ?: "m4a"

                val sanitizedTitle = newTitle
                    .replace(Regex("[^a-zA-Z0-9.-]+"), "_")
                    .trim('_')
                    .let { it.ifBlank { "voicememo" } }
                    .take(100)
                val newDisplayName = "$sanitizedTitle.$extension"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
                }
                context.contentResolver.update(uri, contentValues, null, null)
            }
            val updatedMemo = memo.copy(title = newTitle)
            voiceMemoRepository.update(updatedMemo)
        }
    }

    private fun getDisplayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                }
            }
        }
        return uri.lastPathSegment
    }


    fun deleteVoiceMemo(voiceMemo: VoiceMemo) {
        viewModelScope.launch {
            if (voiceMemo.id == _currentlyPlayingId.value) {
                stopPlayback()
            }
            withContext(Dispatchers.IO) {
                try {
                    if (voiceMemo.filePath.startsWith("content://")) {
                        val uri = voiceMemo.filePath.toUri()
                        context.contentResolver.delete(uri, null, null)
                    } else {
                        val file = File(voiceMemo.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            voiceMemoRepository.delete(voiceMemo.id)
        }
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }
}
