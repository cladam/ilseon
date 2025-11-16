package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.AnalyticsRepository
import com.ilseon.ui.screen.AnalyticsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimeInterval {
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData

    private val _selectedInterval = MutableStateFlow(TimeInterval.WEEK)
    val selectedInterval: StateFlow<TimeInterval> = _selectedInterval

    init {
        loadAnalyticsData()
    }

    fun selectTimeInterval(interval: TimeInterval) {
        _selectedInterval.value = interval
        loadAnalyticsData()
    }

    private fun loadAnalyticsData() {
        viewModelScope.launch {
            _analyticsData.value = null // Show loading indicator
            _analyticsData.value = analyticsRepository.getAnalyticsData(selectedInterval.value)
        }
    }
}