package com.ilseon

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.FocusBlockRepository
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class TaskContextViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var taskContextRepository: TaskContextRepository
    private lateinit var focusBlockRepository: FocusBlockRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        taskContextRepository = mockk()
        focusBlockRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `contextsWithFocusBlock correctly combines data from repositories`() = runTest {
        // Arrange
        val contextId1 = UUID.randomUUID()
        val contextId2 = UUID.randomUUID()

        val context1 = TaskContext(id = contextId1, name = "Work")
        val context2 = TaskContext(id = contextId2, name = "Personal")

        val focusBlock1 = FocusBlock(contextId = contextId1, startTime = "09:00", endTime = "17:00", repeatDays = emptyList())
        
        // Start with empty flows
        val contextsFlow = MutableStateFlow<List<TaskContext>>(emptyList())
        val focusBlocksFlow = MutableStateFlow<List<FocusBlock>>(emptyList())

        coEvery { taskContextRepository.getContexts() } returns contextsFlow
        coEvery { focusBlockRepository.getFocusBlocks() } returns focusBlocksFlow

        // Act
        val viewModel = TaskContextViewModel(taskContextRepository, focusBlockRepository)

        // Assert
        viewModel.contextsWithFocusBlock.test {
            // 1. Await the initial empty state
            assertEquals(emptyList<ContextWithFocusBlock>(), awaitItem())

            // 2. Push new values to the upstream flows
            contextsFlow.value = listOf(context1, context2)
            focusBlocksFlow.value = listOf(focusBlock1)
            
            // 3. Await the new, combined emission
            val combinedList = awaitItem()
            
            assertEquals(2, combinedList.size)

            val workContext = combinedList.find { it.context.name == "Work" }
            assertNotNull(workContext)
            assertNotNull(workContext?.focusBlock)
            assertEquals("09:00", workContext?.focusBlock?.startTime)
            
            val personalContext = combinedList.find { it.context.name == "Personal" }
            assertNotNull(personalContext)
            assertNull(personalContext?.focusBlock) // This context has no matching focus block
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}
