package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskContextViewModel @Inject constructor(
    private val repository: TaskContextRepository
) : ViewModel() {

    val contexts: StateFlow<List<TaskContext>> = repository.getContexts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addContext(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.addContext(name)
            }
        }
    }

    fun deleteContext(id: UUID) {
        viewModelScope.launch {
            repository.deleteContext(id)
        }
    }
}
