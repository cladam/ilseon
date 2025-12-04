package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OngoingTasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _ongoingTasks = MutableStateFlow<List<Task>>(emptyList())
    val ongoingTasks: StateFlow<List<Task>> = _ongoingTasks.asStateFlow()

    fun loadOngoingTasks(contextId: String?) {
        if (contextId == null) return
        viewModelScope.launch {
            taskRepository.getIncompleteTasksByContext(UUID.fromString(contextId)).collect { tasks ->
                _ongoingTasks.value = tasks
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }
}
