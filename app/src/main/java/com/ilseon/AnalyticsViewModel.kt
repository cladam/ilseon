package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.AnalyticsRepository
import com.ilseon.data.task.TaskRepository
import com.ilseon.ui.screen.AnalyticsData
import com.ilseon.ui.screen.DayData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TimeInterval {
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _analyticsData = MutableStateFlow<AnalyticsData?>(null)
    val analyticsData: StateFlow<AnalyticsData?> = _analyticsData

    private val _momentumData = MutableStateFlow<List<DayData>>(emptyList())
    val momentumData: StateFlow<List<DayData>> = _momentumData

    private val _selectedInterval = MutableStateFlow(TimeInterval.WEEK)
    val selectedInterval: StateFlow<TimeInterval> = _selectedInterval

    init {
        loadAnalyticsData()
        loadMomentumData()
    }

    fun selectTimeInterval(interval: TimeInterval) {
        _selectedInterval.value = interval
        loadAnalyticsData()
    }

    private fun loadAnalyticsData() {
        viewModelScope.launch {
            _analyticsData.value = analyticsRepository.getAnalyticsData(selectedInterval.value)
        }
    }

    private fun loadMomentumData() {
        viewModelScope.launch {
            val streakData = taskRepository.getHistoricalCompletionStreaks(7).first()
            val today = LocalDate.now()
            _momentumData.value = (0 until 7).map {
                val date = today.minusDays(it.toLong())
                DayData(date = date, streak = streakData[date] ?: 0)
            }.reversed()
        }
    }
}