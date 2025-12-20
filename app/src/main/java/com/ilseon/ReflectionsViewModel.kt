package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.EnergyLevel
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextRepository
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.userstatus.UserStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReflectionsViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val contextRepository: TaskContextRepository,
    private val userStatusRepository: UserStatusRepository
) : ViewModel() {

    val reflections: StateFlow<List<Task>> = taskRepository.getTasksWithReflections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val contextMap: StateFlow<Map<UUID, TaskContext>> = contextRepository.getContexts()
        .map { contexts -> contexts.associateBy { it.id } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun updateReflection(task: Task, energyLevel: EnergyLevel? = null) {
        viewModelScope.launch {
            val updatedTask = if (energyLevel != null) {
                task.copy(actualEnergyLevel = energyLevel)
            } else {
                task
            }
            taskRepository.updateTask(updatedTask)

            // Also update user's current energy level
            energyLevel?.let {
                userStatusRepository.updateUserEnergyLevel(it)
            }
        }
    }

    fun deleteReflection(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(completionReflection = null)
            taskRepository.updateTask(updatedTask)
        }
    }
}
