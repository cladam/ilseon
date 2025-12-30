package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.service.AudioHandler
import com.ilseon.service.RecordingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.plusAssign

enum class RecorderState {
    Idle,
    Recording,
    Paused,
    Stopped,
    Saving
}

@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val audioHandler: AudioHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecorderState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds = _durationSeconds.asStateFlow()

    private var durationJob: Job? = null
    private var _recordingResult: RecordingResult? = null

    fun startRecording() {
        if (audioHandler.isRecording()) return

        audioHandler.startRecording {
            _uiState.value = RecorderState.Recording
            _durationSeconds.value = 0
            startTimer()
        }
    }

    private fun startTimer() {
        durationJob?.cancel()
        durationJob = viewModelScope.launch {
            while (_uiState.value == RecorderState.Recording) {
                delay(1000)
                _durationSeconds.value += 1
            }
        }
    }

    fun stopRecording(): RecordingResult? {
        durationJob?.cancel()
        val result = audioHandler.stopRecording()
        _recordingResult = result
        return result
    }

    fun getRecordingResult(): RecordingResult? = _recordingResult

    fun resetState() {
        _uiState.value = RecorderState.Idle
        _durationSeconds.value = 0
        _recordingResult = null
        durationJob?.cancel()
    }

    fun pauseRecording() {
        audioHandler.pauseRecording()
        _uiState.value = RecorderState.Paused
        durationJob?.cancel()
    }

    fun resumeRecording() {
        audioHandler.resumeRecording()
        _uiState.value = RecorderState.Recording
        startTimer()
    }

    fun discardRecording() {
        durationJob?.cancel()
        audioHandler.cancelRecording()
        resetState()
    }
}
