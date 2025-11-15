package com.ilseon

import androidx.lifecycle.ViewModel
import com.ilseon.ui.screen.AnalyticsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AnalyticsViewModel : ViewModel() {

    private val _analyticsData = MutableStateFlow(generateMockData())
    val analyticsData: StateFlow<AnalyticsData> = _analyticsData

    private fun generateMockData() = AnalyticsData(
        focusDistribution = mapOf(
            "Work" to 0.55f,
            "Family" to 0.25f,
            "Health" to 0.10f,
            "Personal" to 0.10f
        ),
        averageTimeBlockMinutes = 230,
        averageDurationMinutes = 120,
        topKeywords = listOf(
            "Distracted" to 12,
            "Flow" to 8,
            "Forgot" to 6,
            "Ok" to 4
        )
    )
}
