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

enum class RecorderState {
    Idle,      // Not recording
    Recording, // Actively recording
    Paused,    // Recording is paused
    Stopped    // Recording has stopped, result is available for saving
}

@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val audioHandler: AudioHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecorderState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds = _durationSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var recordingResult: RecordingResult? = null

    fun startRecording() {
        if (_uiState.value != RecorderState.Idle) return
        viewModelScope.launch {
            audioHandler.startRecording()
            _uiState.value = RecorderState.Recording
            startTimer()
        }
    }

    fun pauseRecording() {
        if (_uiState.value != RecorderState.Recording) return
        viewModelScope.launch {
            audioHandler.pauseRecording()
            _uiState.value = RecorderState.Paused
            stopTimer() // Pauses the timer
        }
    }

    fun resumeRecording() {
        if (_uiState.value != RecorderState.Paused) return
        viewModelScope.launch {
            audioHandler.resumeRecording()
            _uiState.value = RecorderState.Recording
            startTimer() // Resumes the timer
        }
    }

    fun stopRecording() {
        if (_uiState.value != RecorderState.Recording && _uiState.value != RecorderState.Paused) return
        recordingResult = audioHandler.stopRecording()
        stopTimer()
        _uiState.value = RecorderState.Stopped
    }

    fun discardRecording() {
        audioHandler.cancelRecording()
        resetState()
    }

    fun getRecordingResult(): RecordingResult? {
        return if (_uiState.value == RecorderState.Stopped) {
            recordingResult
        } else {
            null
        }
    }

    fun resetState() {
        recordingResult = null
        _durationSeconds.value = 0
        _uiState.value = RecorderState.Idle
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value == RecorderState.Recording) {
                delay(1000)
                _durationSeconds.value++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    override fun onCleared() {
        if (_uiState.value == RecorderState.Recording || _uiState.value == RecorderState.Paused) {
            audioHandler.cancelRecording()
        }
        super.onCleared()
    }
}