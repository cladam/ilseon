package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.FocusBlockRepository
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextRepository
import com.ilseon.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ContextWithFocusBlock(
    val context: TaskContext,
    val focusBlock: FocusBlock?
)

@HiltViewModel
class TaskContextViewModel @Inject constructor(
    private val taskContextRepository: TaskContextRepository,
    private val focusBlockRepository: FocusBlockRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _selectedContextTasks = MutableStateFlow<List<Task>>(emptyList())
    val selectedContextTasks = _selectedContextTasks.asStateFlow()

    private val _showTasksDialog = MutableStateFlow(false)
    val showTasksDialog = _showTasksDialog.asStateFlow()

    val contextsWithFocusBlock: StateFlow<List<ContextWithFocusBlock>> =
        taskContextRepository.getContexts()
            .combine(focusBlockRepository.getFocusBlocks()) { contexts, focusBlocks ->
                val focusBlockMap = focusBlocks.associateBy { it.contextId }
                contexts.map { context ->
                    ContextWithFocusBlock(context, focusBlockMap[context.id])
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun addContext(
        name: String,
        description: String?,
        startTime: String?,
        endTime: String?,
        repeatDays: List<Int>?
    ) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                taskContextRepository.addContext(name, description, startTime, endTime, repeatDays)
            }
        }
    }

    fun updateContext(
        id: UUID,
        name: String,
        description: String?,
        startTime: String?,
        endTime: String?,
        repeatDays: List<Int>?
    ) {
        viewModelScope.launch {
            taskContextRepository.updateContext(id, name, description, startTime, endTime, repeatDays)
        }
    }

    fun deleteContext(id: UUID) {
        viewModelScope.launch {
            taskContextRepository.deleteContext(id)
        }
    }

    fun onContextSelected(contextId: UUID) {
        viewModelScope.launch {
            taskRepository.getIncompleteTasksByContext(contextId).collect { tasks ->
                _selectedContextTasks.value = tasks
                _showTasksDialog.value = true
            }
        }
    }

    fun dismissTasksDialog() {
        _showTasksDialog.value = false
    }
}
