package com.ilseon

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ilseon.data.task.SettingsRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.coEvery

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var settingsRepository: SettingsRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        // Mock the flow to prevent issues during ViewModel initialization
        coEvery { settingsRepository.nudgeNotificationsEnabled } returns MutableStateFlow(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setNudgeNotificationsEnabled calls repository`() = runTest {
        // Arrange
        val viewModel = SettingsViewModel(settingsRepository)

        // Act
        viewModel.setNudgeNotificationsEnabled(false)
        advanceUntilIdle() // Ensure the coroutine completes

        // Assert
        coVerify { settingsRepository.setNudgeNotificationsEnabled(false) }
    }
}
