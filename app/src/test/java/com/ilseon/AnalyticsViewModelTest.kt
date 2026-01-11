package com.ilseon

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.ilseon.data.task.AnalyticsRepository
import com.ilseon.data.task.TaskRepository
import com.ilseon.ui.screen.AnalyticsData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@ExperimentalCoroutinesApi
class AnalyticsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var taskRepository: TaskRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        analyticsRepository = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads momentum data correctly`() = runTest(testDispatcher) {
        // Arrange
        val today = LocalDate.now()
        val streakData = mapOf(
            today to 2,
            today.minusDays(1) to 1,
            today.minusDays(3) to 1
        )
        coEvery { taskRepository.getHistoricalCompletionStreaks(7) } returns flowOf(streakData)

        // Act
        val viewModel = AnalyticsViewModel(analyticsRepository, taskRepository)
        advanceUntilIdle()

        // Assert
        viewModel.momentumData.test {
            val loadedData = awaitItem()
            assertEquals(7, loadedData.size)
            assertEquals(2, loadedData.find { it.date == today }?.streak)
            assertEquals(1, loadedData.find { it.date == today.minusDays(1) }?.streak)
            assertEquals(0, loadedData.find { it.date == today.minusDays(2) }?.streak) // Day with no completions
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `selecting time interval calls repository and updates data`() = runTest(testDispatcher) {
        // Arrange
        val weeklyData = AnalyticsData(
            focusDistribution = mapOf("Work" to 0.75f, "Personal" to 0.25f),
            averageTimeBlockMinutes = 60,
            averageDurationMinutes = 30,
            topKeywords = listOf("Test" to 1),
            overdueTasksCount = 5,
            ideasCount = 3,
            voiceMemosCount = 2,
            interruptedTasksCount = 2
        )
        val monthlyData = AnalyticsData(
            focusDistribution = mapOf("Work" to 0.8f, "Personal" to 0.2f),
            averageTimeBlockMinutes = 75,
            averageDurationMinutes = 45,
            topKeywords = listOf("Review" to 5),
            overdueTasksCount = 20,
            ideasCount = 15,
            voiceMemosCount = 10,
            interruptedTasksCount = 10
        )

        coEvery { analyticsRepository.getAnalyticsData(TimeInterval.WEEK) } returns weeklyData
        coEvery { analyticsRepository.getAnalyticsData(TimeInterval.MONTH) } returns monthlyData
        // Required for the init block
        coEvery { taskRepository.getHistoricalCompletionStreaks(any()) } returns flowOf(emptyMap())


        val viewModel = AnalyticsViewModel(analyticsRepository, taskRepository)
        advanceUntilIdle()

        viewModel.analyticsData.test {
            assertEquals(weeklyData, awaitItem())

            viewModel.selectTimeInterval(TimeInterval.MONTH)

            assertEquals(monthlyData, awaitItem())

            coVerify { analyticsRepository.getAnalyticsData(TimeInterval.MONTH) }

            cancelAndIgnoreRemainingEvents()
        }
    }
}
