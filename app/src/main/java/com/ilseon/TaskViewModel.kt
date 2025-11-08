package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(private val taskRepository: TaskRepository) : ViewModel() {

    val tasks: StateFlow<List<Task>> = taskRepository.getTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String, contextId: UUID?, priority: TaskPriority) {
        viewModelScope.launch {
            if (title.isNotBlank() && contextId != null) {
                taskRepository.insertTask(
                    Task(
                        title = title,
                        contextId = contextId,
                        priority = priority
                    )
                )
            }
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(isComplete = true))
        }
    }
}
