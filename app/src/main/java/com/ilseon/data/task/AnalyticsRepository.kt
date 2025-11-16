package com.ilseon.data.task

import com.ilseon.TimeInterval
import com.ilseon.ui.screen.AnalyticsData
import com.ilseon.util.stopWords
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val taskContextDao: TaskContextDao
) {
    suspend fun getAnalyticsData(interval: TimeInterval): AnalyticsData {
        val (startTime, endTime) = calculateTimeRange(interval)

        val focusDistributionRaw = taskDao.getFocusDistribution(startTime, endTime)
        val totalCompletedTasks = focusDistributionRaw.sumOf { it.count }.toFloat()
        val focusDistribution = if (totalCompletedTasks > 0) {
            focusDistributionRaw.mapNotNull {
                val context = taskContextDao.getContext(it.contextId)
                if (context != null) {
                    context.name to it.count / totalCompletedTasks
                } else {
                    null
                }
            }.toMap()
        } else {
            emptyMap()
        }

        val averageTimeBlockMinutes = taskDao.getAverageTimeBlockMillis(startTime, endTime)?.div(60_000)?.toInt() ?: 0
        val averageDurationMinutes = taskDao.getAverageDurationMillis(startTime, endTime)?.div(60_000)?.toInt() ?: 0
        val overdueTasksCount = taskDao.getOverdueTasksCount(startTime, endTime)

        val reflections = taskDao.getCompletionReflections(startTime, endTime)
        val topKeywords = reflections
            .flatMap { it.split(Regex("[\\s.,!?]+")) }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() && it !in stopWords }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(15)

        return AnalyticsData(
            focusDistribution = focusDistribution,
            averageTimeBlockMinutes = averageTimeBlockMinutes,
            averageDurationMinutes = averageDurationMinutes,
            topKeywords = topKeywords,
            overdueTasksCount = overdueTasksCount
        )
    }

    private fun calculateTimeRange(interval: TimeInterval): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        val startTime = when (interval) {
            TimeInterval.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            TimeInterval.MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.timeInMillis
            }
            TimeInterval.YEAR -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.timeInMillis
            }
            TimeInterval.ALL_TIME -> 0L
        }
        return Pair(startTime, endTime)
    }
}