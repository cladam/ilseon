package com.ilseon

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class CompletedTasksViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var taskRepository: TaskRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        taskRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completedTasks StateFlow emits tasks from repository`() = runTest {
        // Arrange
        val completedTask = Task(
            id = UUID.randomUUID(),
            title = "A Finished Task",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.Low,
            isComplete = true
        )
        val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
        coEvery { taskRepository.getCompletedTasks() } returns tasksFlow

        // Act
        val viewModel = CompletedTasksViewModel(taskRepository)

        // Assert
        viewModel.completedTasks.test {
            assertEquals(emptyList<Task>(), awaitItem())

            tasksFlow.value = listOf(completedTask)

            val emittedList = awaitItem()
            assertEquals(1, emittedList.size)
            assertEquals(true, emittedList[0].isComplete)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}
