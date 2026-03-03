package com.ilseon

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.idea.IdeaExporter
import com.ilseon.data.idea.IdeaImporter
import com.ilseon.data.idea.IdeaRepository
import com.ilseon.data.idea.IlseonIdeaParser
import com.ilseon.data.task.IlseonReflectionParser
import com.ilseon.data.task.ReflectionExporter
import com.ilseon.data.task.ReflectionImporter
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.onFailure

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val reflectionExporter: ReflectionExporter,
    private val ideaRepository: IdeaRepository
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

    val mediaButtonTriggerEnabled = settingsRepository.mediaButtonTriggerEnabled
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

    fun setMediaButtonTriggerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMediaButtonTriggerEnabled(enabled)
        }
    }

    fun setSstLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setSstLanguage(language)
        }
    }

    fun exportReflections(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val tasks = taskRepository.getTasksWithReflections().first()
            val exportedData = reflectionExporter.exportReflections(tasks)
            onExported(exportedData)
        }
    }

    fun importReflections(fileContent: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val parser = IlseonReflectionParser()
            val importer = ReflectionImporter(parser)
            val importedContext = taskRepository.getOrCreateImportedContext()
            importer.import(fileContent, importedContext.id).onSuccess { tasks ->
                taskRepository.insertTasks(tasks)
                onSuccess()
            }
            .onFailure { error ->
                    Log.e("Import", "Import failed", error)
                }
        }
    }

    fun exportIdeas(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val ideas = ideaRepository.getIdeas().first()
            val exporter = IdeaExporter()
            val exportedData = exporter.exportIdeas(ideas)
            onExported(exportedData)
        }
    }

    fun importIdeas(fileContent: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val parser = IlseonIdeaParser()
            val importer = IdeaImporter(parser)
            val importedContext = taskRepository.getOrCreateImportedContext()
            Log.d("Import", "Importing to context: ${importedContext.id}")
            importer.import(fileContent, importedContext.id)
                .onSuccess { ideas ->
                    Log.d("Import", "Parsed ${ideas.size} ideas")
                    ideaRepository.insertIdeas(ideas)
                    Log.d("Import", "Ideas inserted")
                    onSuccess()
                }
                .onFailure { error ->
                    Log.e("Import", "Import failed", error)
                }
        }
    }

    val apiKey = settingsRepository.apiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.setApiKey(key)
        }
    }
}
