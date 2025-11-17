package com.ilseon

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ilseon.data.task.NoteExporter
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.task.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var noteExporter: NoteExporter
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        taskRepository = mockk(relaxed = true)
        noteExporter = mockk(relaxed = true)

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
        val viewModel = SettingsViewModel(settingsRepository, taskRepository, noteExporter)

        // Act
        viewModel.setNudgeNotificationsEnabled(false)
        advanceUntilIdle() // Ensure the coroutine completes

        // Assert
        coVerify { settingsRepository.setNudgeNotificationsEnabled(false) }
    }

    @Test
    fun `exportNotes calls noteExporter`() = runTest {
        // Arrange
        val tasks = listOf(mockk<com.ilseon.data.task.Task>())
        coEvery { taskRepository.getTasksWithReflections() } returns flowOf(tasks)
        coEvery { noteExporter.exportNotes(tasks) } returns "exported data"
        val viewModel = SettingsViewModel(settingsRepository, taskRepository, noteExporter)
        var exportedData = ""

        // Act
        viewModel.exportNotes { exportedData = it }
        advanceUntilIdle()

        // Assert
        coVerify { noteExporter.exportNotes(tasks) }
        assert(exportedData == "exported data")
    }
}