package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ilseon.task.Task
import com.ilseon.task.TaskContext
import com.ilseon.task.TaskDao
import com.ilseon.task.TaskPriority
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val taskDao: TaskDao) : ViewModel() {

    val tasks = taskDao.getIncompleteTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addTask(title: String, context: TaskContext, priority: TaskPriority) {
        viewModelScope.launch {
            val task = Task(title = title, context = context, priority = priority)
            taskDao.insert(task)
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task.copy(isComplete = true))
        }
    }
}

class TaskViewModelFactory(private val taskDao: TaskDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(taskDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
