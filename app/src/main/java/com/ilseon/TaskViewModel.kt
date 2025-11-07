package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val tasks = taskRepository.getIncompleteTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addTask(title: String, context: TaskContext, priority: TaskPriority) {
        viewModelScope.launch {
            val task = Task(title = title, context = context, priority = priority)
            taskRepository.insert(task)
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            taskRepository.update(task.copy(isComplete = true))
        }
    }
}
