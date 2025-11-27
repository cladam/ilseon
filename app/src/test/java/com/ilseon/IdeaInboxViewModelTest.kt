package com.ilseon

import app.cash.turbine.test
import com.ilseon.data.idea.Idea
import com.ilseon.data.idea.IdeaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

@ExperimentalCoroutinesApi
class IdeaInboxViewModelTest {

    private lateinit var viewModel: IdeaInboxViewModel
    private lateinit var ideaRepository: IdeaRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ideaRepository = mockk(relaxed = true)
        viewModel = IdeaInboxViewModel(ideaRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ideas StateFlow should emit ideas from repository`() = runTest {
        // Given
        val testIdeas = listOf(Idea(content = "Test Idea 1"), Idea(content = "Test Idea 2"))
        coEvery { ideaRepository.getIdeas() } returns flowOf(testIdeas)

        // When
        viewModel = IdeaInboxViewModel(ideaRepository) // Re-init to trigger collection

        // Then
        viewModel.ideas.test {
            // Skip the initial empty list emitted by the StateFlow
            skipItems(1)
            // Now assert against the actual emission from the repository
            assertEquals(testIdeas, awaitItem())
        }
    }

    @Test
    fun `addIdea should call insertIdea on repository`() = runTest {
        // Given
        val content = "New test idea"

        // When
        viewModel.addIdea(content)
        testDispatcher.scheduler.advanceUntilIdle()


        // Then
        coVerify { ideaRepository.insertIdea(content) }
    }

    @Test
    fun `updateIdea should call updateIdea on repository`() = runTest {
        // Given
        val idea = Idea(content = "Updated idea")

        // When
        viewModel.updateIdea(idea)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { idea.content?.let { ideaRepository.updateIdea(idea.id, it) } }
    }

    @Test
    fun `deleteIdea should call deleteIdea on repository`() = runTest {
        // Given
        val idea = Idea()

        // When
        viewModel.deleteIdea(idea)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { ideaRepository.deleteIdea(idea.id) }
    }

    @Test
    fun `convertToTask should call convertIdea on repository`() = runTest {
        // Given
        val idea = Idea()

        // When
        viewModel.convertToTask(idea)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { ideaRepository.convertIdea(idea.id) }
    }
}