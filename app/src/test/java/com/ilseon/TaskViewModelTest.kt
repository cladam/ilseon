package com.ilseon

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.notifications.ReminderManager
import com.ilseon.service.HapticManager
import com.ilseon.service.NotificationService
import com.ilseon.service.SoundManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class TaskViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    // Mocks and dispatcher are declared here
    private lateinit var taskRepository: TaskRepository
    private lateinit var hapticManager: HapticManager
    private lateinit var soundManager: SoundManager
    private lateinit var notificationService: NotificationService
    private lateinit var reminderManager: ReminderManager
    private lateinit var settingsRepository: SettingsRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Initialize mocks in setUp
        taskRepository = mockk(relaxed = true)
        hapticManager = mockk(relaxed = true)
        soundManager = mockk(relaxed = true)
        notificationService = mockk(relaxed = true)
        reminderManager = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        coEvery { taskRepository.getActiveFocusBlock() } returns MutableStateFlow<FocusBlock?>(null)
        coEvery { taskRepository.getIncompleteTasks() } returns MutableStateFlow<List<Task>>(emptyList())
        coEvery { taskRepository.getAllFocusBlocks() } returns emptyList()
        coEvery { taskRepository.getRunningTasks() } returns emptyList()
        coEvery { settingsRepository.naggingNotificationsEnabled } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addTask with time block creates and inserts a new task`() = runTest(testDispatcher.scheduler) {
        val viewModel = TaskViewModel(taskRepository, hapticManager, soundManager, notificationService, reminderManager, settingsRepository)
        val taskSlot = slot<Task>()
        coEvery { taskRepository.insertTask(capture(taskSlot)) } just runs

        viewModel.addTask(
            title = "Test Task with Time Block",
            description = "Description",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.High,
            startTimeStr = "10:00",
            endTimeStr = "11:00",
            durationInMinutes = null,
            isRecurring = false,
            recurrenceDays = emptySet()
        )
        runCurrent()

        coVerify { taskRepository.insertTask(any()) }
        val capturedTask = taskSlot.captured
        assert(capturedTask.title == "Test Task with Time Block")
        assert(capturedTask.startTime != null)
        assert(capturedTask.endTime != null)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `addTask with duration creates and inserts a new task`() = runTest(testDispatcher.scheduler) {
        val viewModel = TaskViewModel(taskRepository, hapticManager, soundManager, notificationService, reminderManager, settingsRepository)
        val taskSlot = slot<Task>()
        coEvery { taskRepository.insertTask(capture(taskSlot)) } just runs

        viewModel.addTask(
            title = "Test Task with Duration",
            description = "Description",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.Medium,
            startTimeStr = "",
            endTimeStr = "",
            durationInMinutes = 45,
            isRecurring = false,
            recurrenceDays = emptySet()
        )
        runCurrent()

        coVerify { taskRepository.insertTask(any()) }
        val capturedTask = taskSlot.captured
        assert(capturedTask.title == "Test Task with Duration")
        assert(capturedTask.totalTimeInMinutes == 45)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `addTask with basic info creates and inserts a new task`() = runTest(testDispatcher.scheduler) {
        val viewModel = TaskViewModel(taskRepository, hapticManager, soundManager, notificationService, reminderManager, settingsRepository)
        val taskSlot = slot<Task>()
        coEvery { taskRepository.insertTask(capture(taskSlot)) } just runs

        viewModel.addTask(
            title = "Basic Test Task",
            description = "Just a title and description",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.Low,
            startTimeStr = "",
            endTimeStr = "",
            durationInMinutes = null,
            isRecurring = false,
            recurrenceDays = emptySet()
        )
        runCurrent()

        coVerify { taskRepository.insertTask(any()) }
        val capturedTask = taskSlot.captured
        assert(capturedTask.title == "Basic Test Task")
        assert(capturedTask.description == "Just a title and description")
        assert(capturedTask.totalTimeInMinutes == null)
        assert(capturedTask.startTime == null)
        assert(capturedTask.endTime == null)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `completeTask updates task to complete and sets reflection`() = runTest(testDispatcher.scheduler) {
        val viewModel = TaskViewModel(taskRepository, hapticManager, soundManager, notificationService, reminderManager, settingsRepository)
        val taskSlot = slot<Task>()
        coEvery { taskRepository.updateTask(capture(taskSlot)) } just runs

        val originalTask = Task(
            id = UUID.randomUUID(),
            title = "Task to be completed",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.Medium
        )
        val reflection = "This was a test task."

        viewModel.completeTask(originalTask, reflection)
        runCurrent()

        coVerify { taskRepository.updateTask(any()) }
        val capturedTask = taskSlot.captured
        assert(capturedTask.isComplete)
        assert(capturedTask.completionReflection == reflection)
        assert(capturedTask.completedAt != null)

        viewModel.viewModelScope.cancel()
    }
}