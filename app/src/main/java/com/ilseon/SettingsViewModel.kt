package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.NoteExporter
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val noteExporter: NoteExporter
) : ViewModel() {

    val nudgeNotificationsEnabled = settingsRepository.nudgeNotificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val naggingNotificationsEnabled = settingsRepository.naggingNotificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val bluetoothSstEnabled = settingsRepository.bluetoothSstEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val sstLanguage = settingsRepository.sstLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en-GB"
        )

    fun setNudgeNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNudgeNotificationsEnabled(enabled)
        }
    }

    fun setNaggingNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNaggingNotificationsEnabled(enabled)
        }
    }

    fun setBluetoothSstEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBluetoothSstEnabled(enabled)
        }
    }

    fun setSstLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setSstLanguage(language)
        }
    }

    fun exportNotes(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val tasks = taskRepository.getTasksWithReflections().first()
            val exportedData = noteExporter.exportNotes(tasks)
            onExported(exportedData)
        }
    }
}
