package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.FocusBlockRepository
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val focusBlockRepository: FocusBlockRepository
) : ViewModel() {

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

    fun deleteContext(id: UUID) {
        viewModelScope.launch {
            taskContextRepository.deleteContext(id)
        }
    }
}
