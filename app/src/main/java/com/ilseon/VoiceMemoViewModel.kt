package com.ilseon

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.data.voicememo.VoiceMemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
            started = SharingStarted.Companion.WhileSubscribed(5000),
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
                setDataSource(memo.filePath)
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
            val voiceMemo = VoiceMemo(
                transcription = transcription,
                filePath = filePath,
                durationSeconds = durationSeconds
            )
            voiceMemoRepository.insert(voiceMemo)
        }
    }

    fun deleteVoiceMemo(voiceMemo: VoiceMemo) {
        viewModelScope.launch {
            if (voiceMemo.id == _currentlyPlayingId.value) {
                stopPlayback()
            }
            withContext(Dispatchers.IO) {
                try {
                    val file = File(voiceMemo.filePath)
                    if (file.exists()) {
                        file.delete()
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
                title = voiceMemo.transcription,
                description = "Converted from voice memo.",
                priority = TaskPriority.Medium,
                contextId = UUID.randomUUID() // Or fetch a default context
            )
            taskRepository.insertTask(task)
            withContext(Dispatchers.IO) {
                try {
                    val file = File(voiceMemo.filePath)
                    if (file.exists()) {
                        file.delete()
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