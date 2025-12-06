package com.ilseon

import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.data.voicememo.VoiceMemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class VoiceMemoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceMemoRepository: VoiceMemoRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null
    private var progressTrackerJob: Job? = null

    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId = _currentlyPlayingId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress = _playbackProgress.asStateFlow()

    val voiceMemos = voiceMemoRepository.getVoiceMemos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
                    Uri.parse(memo.filePath)
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

    fun saveVoiceMemo(filePath: String, transcription: String, durationSeconds: Int) {
        viewModelScope.launch {
            val title = if (transcription.isNotBlank() && transcription != "(Transcription failed)") {
                transcription.substringBefore('\n').take(50)
            } else {
                "Voice Memo"
            }

            val finalUri = saveRecordingToMediaStore(filePath, title)

            if (finalUri != null) {
                val voiceMemo = VoiceMemo(
                    title = title,
                    transcription = transcription,
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
            .let { if (it.isBlank()) "voicememo" else it }
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
                put(MediaStore.MediaColumns.RELATIVE_PATH, recordingsFolder + "/ilseon")
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
                val uri = Uri.parse(memo.filePath)
                val sanitizedTitle = newTitle
                    .replace(Regex("[^a-zA-Z0-9.-]+"), "_")
                    .trim('_')
                    .let { if (it.isBlank()) "voicememo" else it }
                    .take(100)

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$sanitizedTitle.m4a")
                }
                context.contentResolver.update(uri, contentValues, null, null)
            }
            // Note: Renaming for old file paths is complex and not handled here to avoid more issues.
            // The title in the database will be updated regardless.
            val updatedMemo = memo.copy(title = newTitle)
            voiceMemoRepository.update(updatedMemo)
        }
    }

    fun deleteVoiceMemo(voiceMemo: VoiceMemo) {
        viewModelScope.launch {
            if (voiceMemo.id == _currentlyPlayingId.value) {
                stopPlayback()
            }
            withContext(Dispatchers.IO) {
                try {
                    if (voiceMemo.filePath.startsWith("content://")) {
                        val uri = Uri.parse(voiceMemo.filePath)
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

    fun convertToTask(voiceMemo: VoiceMemo) {
        viewModelScope.launch {
            if (voiceMemo.id == _currentlyPlayingId.value) {
                stopPlayback()
            }
            val task = Task(
                title = voiceMemo.title,
                description = voiceMemo.transcription,
                priority = TaskPriority.Medium,
                contextId = UUID.randomUUID() // Or fetch a default context
            )
            taskRepository.insertTask(task)
            deleteVoiceMemo(voiceMemo) // Re-use delete logic
        }
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }
}
