package com.ilseon.data.task

import android.content.Context
import com.ilseon.TimeInterval
import com.ilseon.data.idea.IdeaDao
import com.ilseon.data.voicememo.VoiceMemoDao
import com.ilseon.ui.screen.AnalyticsData
import com.ilseon.util.UsageStatsReader
import com.ilseon.util.allStopWords
import com.ilseon.util.cleanTextForAnalysis
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val taskContextDao: TaskContextDao,
    private val ideaDao: IdeaDao,
    private val voiceMemoDao: VoiceMemoDao
) {
    private val usageStatsReader = UsageStatsReader(context)

    suspend fun getAnalyticsData(interval: TimeInterval): AnalyticsData {
        val (startTime, endTime) = calculateTimeRange(interval)

        val taskDistribution = taskDao.getFocusDistribution(startTime, endTime)
        val ideaDistribution = ideaDao.getContextDistribution(startTime, endTime)
        val voiceMemoDistribution = voiceMemoDao.getContextDistribution(startTime, endTime)

        val combinedDistribution = (taskDistribution.map { it.contextId to it.count } +
                ideaDistribution.map { it.contextId to it.count } +
                voiceMemoDistribution.map { it.contextId to it.count })
            .groupBy { it.first }
            .mapValues { entry -> entry.value.sumOf { it.second } }

        val totalItems = combinedDistribution.values.sum().toFloat()
        val focusDistribution = if (totalItems > 0) {
            combinedDistribution.mapNotNull { (contextId, count) ->
                val context = contextId?.let { taskContextDao.getContext(it) }
                if (context != null) {
                    context.name to count / totalItems
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
            .flatMap { cleanTextForAnalysis(it).split(Regex("\\s+")) }
            .filter { it.isNotBlank() && it !in allStopWords }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(9) // Just show top 9 words

        val completedTasks = taskDao.getCompletedTasks(startTime, endTime)
        var interruptedTasksCount = 0
        if (usageStatsReader.hasUsageStatsPermission()) {
            completedTasks.forEach { task ->
                val taskStartTime = task.startTime
                val taskCompletedAt = task.completedAt
                val taskDueTime = task.dueTime
                val totalTime = task.totalTimeInMinutes

                if (taskCompletedAt != null) {
                    val pickups = if (taskStartTime != null) {
                        // Time Block based task
                        usageStatsReader.getPhonePickups(taskStartTime, taskCompletedAt)
                    } else if (taskDueTime != null && totalTime != null) {
                        // Duration based task
                        val calculatedStartTime = taskDueTime - (totalTime * 60 * 1000L)
                        usageStatsReader.getPhonePickups(calculatedStartTime, taskCompletedAt)
                    } else {
                        0
                    }

                    if (pickups > 1) { // > 1 because starting the task counts as 1
                        interruptedTasksCount++
                    }
                }
            }
        }

        val ideasCount = ideaDao.getIdeasCount(startTime, endTime)
        val voiceMemosCount = voiceMemoDao.getVoiceMemosCount(startTime, endTime)

        return AnalyticsData(
            focusDistribution = focusDistribution,
            averageTimeBlockMinutes = averageTimeBlockMinutes,
            averageDurationMinutes = averageDurationMinutes,
            topKeywords = topKeywords,
            overdueTasksCount = overdueTasksCount,
            interruptedTasksCount = interruptedTasksCount,
            ideasCount = ideasCount,
            voiceMemosCount = voiceMemosCount
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
