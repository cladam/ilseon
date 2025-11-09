package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CompletedTasksViewModel @Inject constructor(
    taskRepository: TaskRepository
) : ViewModel() {

    val completedTasks: StateFlow<List<Task>> = taskRepository.getCompletedTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
