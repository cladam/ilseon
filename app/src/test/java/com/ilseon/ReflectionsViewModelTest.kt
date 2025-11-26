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
class ReflectionsViewModelTest {

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
    fun `reflections StateFlow emits tasks with reflections from repository`() = runTest {
        // 1. Arrange
        val completedTask = Task(
            id = UUID.randomUUID(),
            title = "Completed Task",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.High,
            isComplete = true,
            completionReflection = "This is a test reflection."
        )
        // Start with an empty flow
        val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
        coEvery { taskRepository.getTasksWithReflections() } returns tasksFlow

        // 2. Act
        val viewModel = ReflectionsViewModel(taskRepository)

        // 3. Assert
        viewModel.reflections.test {
            // First, assert the initial empty state is emitted.
            assertEquals(emptyList<Task>(), awaitItem())

            // Now, have the repository emit the new list of tasks.
            tasksFlow.value = listOf(completedTask)

            // Await the new emission and assert its contents.
            val emittedList = awaitItem()
            assertEquals(1, emittedList.size)
            assertEquals("This is a test reflection.", emittedList[0].completionReflection)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}
