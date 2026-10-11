package com.ilseon

import android.content.Context
import com.ilseon.data.routine.Routine
import com.ilseon.data.routine.RoutineRepository
import com.ilseon.data.routine.RoutineStep
import com.ilseon.data.routine.RoutineWithSteps
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextRepository
import com.ilseon.data.routine.RoutineRun
import com.ilseon.service.HapticManager
import com.ilseon.data.task.FocusBlockRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.UUID

class RoutineViewModelTest {

    private val routineRepository: RoutineRepository = mockk(relaxed = true)
    private val contextRepository: TaskContextRepository = mockk(relaxed = true)
    private val focusBlockRepository: FocusBlockRepository = mockk(relaxed = true)
    private val hapticManager: HapticManager = mockk(relaxed = true)
    private val mockContext: Context = mockk(relaxed = true)
    private lateinit var viewModel: RoutineViewModel

    private val contextId1 = UUID.randomUUID()
    private val contextId2 = UUID.randomUUID()

    @Before
    fun setup() {
        coEvery { routineRepository.observeActiveRoutines() } returns flowOf(emptyList())
        coEvery { contextRepository.getContexts() } returns flowOf(emptyList())
        coEvery { focusBlockRepository.getFocusBlocks() } returns flowOf(emptyList())
        viewModel = RoutineViewModel(routineRepository, contextRepository, focusBlockRepository, hapticManager, mockContext)
    }

    @Test
    fun `getDashboardReadyRoutine returns null when time is before startHour`() {
        val routine = RoutineWithSteps(
            routine = Routine(
                title = "Morning Routine",
                contextId = contextId1,
                startHour = 7,
                startMinute = 0,
                endHour = 10,
                endMinute = 0
            ),
            steps = listOf(RoutineStep(routineId = UUID.randomUUID(), title = "Step 1"))
        )

        // Simulate 06:30 AM
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 30)
        }

        val result = viewModel.getDashboardRoutineState(
            routines = listOf(routine),
            todayRuns = emptyList(),
            activeFocusBlock = null,
            nowCalendar = cal
        ).selectedHeroRoutine

        assertNull(result)
    }

    @Test
    fun `getDashboardRoutineState returns routine when within start and end time`() {
        val routine = RoutineWithSteps(
            routine = Routine(
                title = "Morning Routine",
                contextId = contextId1,
                startHour = 7,
                startMinute = 0,
                endHour = 10,
                endMinute = 0
            ),
            steps = listOf(RoutineStep(routineId = UUID.randomUUID(), title = "Step 1"))
        )

        // Simulate 07:15 AM
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 15)
        }

        val result = viewModel.getDashboardRoutineState(
            routines = listOf(routine),
            todayRuns = emptyList(),
            activeFocusBlock = null,
            nowCalendar = cal
        ).selectedHeroRoutine

        assertNotNull(result)
        assertEquals("Morning Routine", result?.first?.routine?.title)
    }

    @Test
    fun `getDashboardRoutineState filters by active focus block context`() {
        val routine = RoutineWithSteps(
            routine = Routine(
                title = "Work Routine",
                contextId = contextId1,
                startHour = 7,
                startMinute = 0
            ),
            steps = listOf(RoutineStep(routineId = UUID.randomUUID(), title = "Step 1"))
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
        }

        // Active focus block is for contextId2 (different context)
        val focusBlock = FocusBlock(
            contextId = contextId2,
            startTime = "06:00",
            endTime = "12:00"
        )

        val result = viewModel.getDashboardRoutineState(
            routines = listOf(routine),
            todayRuns = emptyList(),
            activeFocusBlock = focusBlock,
            nowCalendar = cal
        ).selectedHeroRoutine

        assertNull(result)
    }

    @Test
    fun `getDashboardRoutineState moves completed routine to completed list and promotes uncompleted one`() {
        val routine1 = RoutineWithSteps(
            routine = Routine(
                id = UUID.randomUUID(),
                title = "Routine 1",
                contextId = contextId1
            ),
            steps = listOf(RoutineStep(routineId = UUID.randomUUID(), title = "Step 1"))
        )
        val routine2 = RoutineWithSteps(
            routine = Routine(
                id = UUID.randomUUID(),
                title = "Routine 2",
                contextId = contextId1
            ),
            steps = listOf(RoutineStep(routineId = UUID.randomUUID(), title = "Step 2"))
        )

        // Routine 1 is completed today
        val run1 = RoutineRun(
            routineId = routine1.routine.id,
            sessionDate = System.currentTimeMillis(),
            completedAt = System.currentTimeMillis()
        )

        val state = viewModel.getDashboardRoutineState(
            routines = listOf(routine1, routine2),
            todayRuns = listOf(run1),
            activeFocusBlock = null
        )

        // Routine 1 is in completed list
        assertEquals(1, state.completedRoutines.size)
        assertEquals("Routine 1", state.completedRoutines.first().first.routine.title)

        // Routine 2 is promoted to Hero
        assertNotNull(state.selectedHeroRoutine)
        assertEquals("Routine 2", state.selectedHeroRoutine?.first?.routine?.title)
    }

    @Test
    fun `getDashboardRoutineState prioritizes in-progress routine over not-yet-started routines`() {
        val routine1 = RoutineWithSteps(
            routine = Routine(
                id = UUID.randomUUID(),
                title = "Not started yet",
                contextId = contextId1
            ),
            steps = listOf(RoutineStep(routineId = UUID.randomUUID(), title = "Step 1"))
        )
        val routine2 = RoutineWithSteps(
            routine = Routine(
                id = UUID.randomUUID(),
                title = "In Progress",
                contextId = contextId1
            ),
            steps = listOf(RoutineStep(routineId = UUID.randomUUID(), title = "Step 2"))
        )

        // Routine 2 has progress (step 2)
        val run2 = RoutineRun(
            routineId = routine2.routine.id,
            sessionDate = System.currentTimeMillis(),
            currentStepIndex = 1
        )

        val state = viewModel.getDashboardRoutineState(
            routines = listOf(routine1, routine2),
            todayRuns = listOf(run2),
            activeFocusBlock = null
        )

        assertNotNull(state.selectedHeroRoutine)
        assertEquals("In Progress", state.selectedHeroRoutine?.first?.routine?.title)
    }

    @Test
    fun `getDashboardRoutineState hides routine whose context has a scheduled focus block when that block is inactive`() {
        val routineInScheduledContext = RoutineWithSteps(
            routine = Routine(
                id = UUID.randomUUID(),
                title = "Monday Evening Routine",
                contextId = contextId1
            ),
            steps = listOf(RoutineStep(routineId = UUID.randomUUID(), title = "Step 1"))
        )
        val routineInOpenContext = RoutineWithSteps(
            routine = Routine(
                id = UUID.randomUUID(),
                title = "General Daily Routine",
                contextId = contextId2
            ),
            steps = listOf(RoutineStep(routineId = UUID.randomUUID(), title = "Step 1"))
        )

        // Context 1 has a focus block scheduled, Context 2 has no focus blocks
        val scheduledBlockForContext1 = FocusBlock(
            contextId = contextId1,
            startTime = "18:00",
            endTime = "21:00",
            repeatDays = listOf(1) // Monday
        )

        // When NO focus block is active right now:
        val state = viewModel.getDashboardRoutineState(
            routines = listOf(routineInScheduledContext, routineInOpenContext),
            todayRuns = emptyList(),
            activeFocusBlock = null,
            allFocusBlocks = listOf(scheduledBlockForContext1)
        )

        // Monday Evening Routine must be filtered out! Only the open General routine is eligible.
        assertEquals(1, state.candidateRoutines.size)
        assertEquals("General Daily Routine", state.selectedHeroRoutine?.first?.routine?.title)
    }
}
