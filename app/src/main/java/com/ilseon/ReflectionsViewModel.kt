package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReflectionsViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val reflections: StateFlow<List<Task>> = taskRepository.getTasksWithReflections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateReflection(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun deleteReflection(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(completionReflection = null)
            taskRepository.updateTask(updatedTask)
        }
    }
}