package com.ilseon

import com.ilseon.data.routine.Routine
import com.ilseon.data.routine.RoutineRepository
import com.ilseon.data.routine.RoutineStep
import com.ilseon.data.routine.RoutineWithSteps
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextRepository
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
    private lateinit var viewModel: RoutineViewModel

    private val contextId1 = UUID.randomUUID()
    private val contextId2 = UUID.randomUUID()

    @Before
    fun setup() {
        coEvery { routineRepository.observeActiveRoutines() } returns flowOf(emptyList())
        coEvery { contextRepository.getContexts() } returns flowOf(emptyList())
        viewModel = RoutineViewModel(routineRepository, contextRepository)
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

        val result = viewModel.getDashboardReadyRoutine(
            routines = listOf(routine),
            activeFocusBlock = null,
            nowCalendar = cal
        )

        assertNull(result)
    }

    @Test
    fun `getDashboardReadyRoutine returns routine when within start and end time`() {
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

        val result = viewModel.getDashboardReadyRoutine(
            routines = listOf(routine),
            activeFocusBlock = null,
            nowCalendar = cal
        )

        assertNotNull(result)
        assertEquals("Morning Routine", result?.routine?.title)
    }

    @Test
    fun `getDashboardReadyRoutine filters by active focus block context`() {
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

        val result = viewModel.getDashboardReadyRoutine(
            routines = listOf(routine),
            activeFocusBlock = focusBlock,
            nowCalendar = cal
        )

        assertNull(result)
    }
}
