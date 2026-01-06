package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.idea.Idea
import com.ilseon.data.idea.IdeaRepository
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
class IdeaInboxViewModel @Inject constructor(
    private val ideaRepository: IdeaRepository,
    private val taskContextRepository: TaskContextRepository
) : ViewModel() {

    val ideas: StateFlow<List<Idea>> = ideaRepository.getIdeas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val taskContexts: StateFlow<List<TaskContext>> = taskContextRepository.getContexts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addIdea(content: String, imageUris: List<String> = emptyList(), contextId: UUID? = null) {
        viewModelScope.launch {
            ideaRepository.insertIdea(content, imageUris, contextId = contextId)
        }
    }

    fun updateIdea(idea: Idea) {
        viewModelScope.launch {
            ideaRepository.updateIdea(idea)
        }
    }

    fun deleteIdea(idea: Idea) {
        viewModelScope.launch {
            ideaRepository.deleteIdea(idea.id)
        }
    }

    fun convertToTask(idea: Idea) {
        viewModelScope.launch {
            ideaRepository.convertIdea(idea.id)
        }
    }

    fun saveAsReference(idea: Idea) {
        viewModelScope.launch {
            ideaRepository.updateIdea(idea.copy(isReference = true))
        }
    }

    fun togglePin(idea: Idea) {
        viewModelScope.launch {
            ideaRepository.updateIdea(idea.copy(isPinned = !idea.isPinned))
        }
    }

    fun increaseWeight(idea: Idea) {
        viewModelScope.launch {
            val currentMax = ideas.value.maxOfOrNull { it.weight } ?: 0
            ideaRepository.updateIdea(idea.copy(weight = currentMax + 1))
        }
    }

    fun decreaseWeight(idea: Idea) {
        viewModelScope.launch {
            val currentMin = ideas.value.minOfOrNull { it.weight } ?: 0
            ideaRepository.updateIdea(idea.copy(weight = currentMin - 1))
        }
    }

}
