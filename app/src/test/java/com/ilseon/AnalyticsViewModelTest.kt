package com.ilseon

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.ilseon.data.task.AnalyticsRepository
import com.ilseon.ui.screen.AnalyticsData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AnalyticsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var analyticsRepository: AnalyticsRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        analyticsRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
            interruptedTasksCount = 2
        )
        val monthlyData = AnalyticsData(
            focusDistribution = mapOf("Work" to 0.8f, "Personal" to 0.2f),
            averageTimeBlockMinutes = 75,
            averageDurationMinutes = 45,
            topKeywords = listOf("Review" to 5),
            overdueTasksCount = 20,
            interruptedTasksCount = 10
        )

        coEvery { analyticsRepository.getAnalyticsData(TimeInterval.WEEK) } returns weeklyData
        coEvery { analyticsRepository.getAnalyticsData(TimeInterval.MONTH) } returns monthlyData

        val viewModel = AnalyticsViewModel(analyticsRepository)
        advanceUntilIdle() // Let the initial loadAnalyticsData() in init {} finish

        viewModel.analyticsData.test {
            // Assert initial state from init{}
            assertEquals(weeklyData, awaitItem())

            // Act: Change the time interval
            viewModel.selectTimeInterval(TimeInterval.MONTH)

            // Assert new data
            assertEquals(monthlyData, awaitItem())

            // Verify the repository was called with the new interval
            coVerify { analyticsRepository.getAnalyticsData(TimeInterval.MONTH) }

            cancelAndIgnoreRemainingEvents()
        }
    }
}
