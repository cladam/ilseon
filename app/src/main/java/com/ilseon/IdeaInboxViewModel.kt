package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.idea.Idea
import com.ilseon.data.idea.IdeaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdeaInboxViewModel @Inject constructor(
    private val ideaRepository: IdeaRepository
) : ViewModel() {

    val ideas: StateFlow<List<Idea>> = ideaRepository.getIdeas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addIdea(content: String) {
        viewModelScope.launch {
            ideaRepository.insertIdea(content)
        }
    }

    fun updateIdea(idea: Idea) {
        viewModelScope.launch {
            idea.content?.let { ideaRepository.updateIdea(idea.id, it) }
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
}