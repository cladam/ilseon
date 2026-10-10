package com.ilseon.data.routine

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Calendar
import java.util.UUID

class RoutineRepositoryUnitTest {

    private lateinit var routineDao: RoutineDao
    private lateinit var repository: RoutineRepositoryImpl

    @Before
    fun setup() {
        routineDao = mock(RoutineDao::class.java)
        repository = RoutineRepositoryImpl(routineDao)
    }

    @Test
    fun testCalculateSessionDate_respects0400Cutoff() {
        // Case 1: Tuesday at 02:30 AM (before 04:00 AM cutoff) -> belongs to Monday 04:00 AM
        val tuesday0230 = Calendar.getInstance().apply {
            set(2025, Calendar.MAY, 13, 2, 30, 0) // Tuesday May 13 02:30
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val expectedMonday0400 = Calendar.getInstance().apply {
            set(2025, Calendar.MAY, 12, 4, 0, 0) // Monday May 12 04:00
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sessionDate1 = repository.calculateSessionDate(tuesday0230, cutoffHour = 4)
        assertEquals(expectedMonday0400, sessionDate1)

        // Case 2: Tuesday at 04:00 AM exactly -> belongs to Tuesday 04:00 AM
        val tuesday0400 = Calendar.getInstance().apply {
            set(2025, Calendar.MAY, 13, 4, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sessionDate2 = repository.calculateSessionDate(tuesday0400, cutoffHour = 4)
        assertEquals(tuesday0400, sessionDate2)

        // Case 3: Tuesday at 23:45 PM (evening routine) -> belongs to Tuesday 04:00 AM
        val tuesday2345 = Calendar.getInstance().apply {
            set(2025, Calendar.MAY, 13, 23, 45, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sessionDate3 = repository.calculateSessionDate(tuesday2345, cutoffHour = 4)
        assertEquals(tuesday0400, sessionDate3)

        // Case 4: Wednesday at 03:59 AM (night owl completing evening routine) -> still belongs to Tuesday 04:00 AM!
        val wednesday0359 = Calendar.getInstance().apply {
            set(2025, Calendar.MAY, 14, 3, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sessionDate4 = repository.calculateSessionDate(wednesday0359, cutoffHour = 4)
        assertEquals(tuesday0400, sessionDate4)
    }

    @Test
    fun testCreateRoutine_assignsStepsAndIndices() {
        runBlocking {
            val routineId = UUID.randomUUID()
            val contextId = UUID.randomUUID()
            val routine = Routine(
                id = routineId,
                title = "Morning Launchpad",
                contextId = contextId
            )
            val step1 = RoutineStep(
                routineId = routineId,
                title = "Drink Water",
                targetDurationMinutes = 1
            )
            val step2 = RoutineStep(
                routineId = routineId,
                title = "Take Meds",
                targetDurationMinutes = 2
            )

            val createdId = repository.createRoutine(routine, listOf(step1, step2))
            assertEquals(routineId, createdId)

            verify(routineDao).insertRoutine(routine)
            verify(routineDao).insertSteps(
                listOf(
                    step1.copy(orderIndex = 0),
                    step2.copy(orderIndex = 1)
                )
            )
        }
    }

    @Test
    fun testRecordStepCompletedAndSkipped() {
        runBlocking {
            val runId = UUID.randomUUID()
            val routineId = UUID.randomUUID()
            val step1Id = UUID.randomUUID()
            val step2Id = UUID.randomUUID()

            val initialRun = RoutineRun(
                id = runId,
                routineId = routineId,
                sessionDate = 1000L,
                currentStepIndex = 0
            )

            `when`(routineDao.getRunById(runId)).thenReturn(initialRun)

            // Step 1 completed
            repository.recordStepCompleted(runId, step1Id, nextStepIndex = 1)
            val expectedCompletedRun = initialRun.copy(
                completedStepIds = listOf(step1Id.toString()),
                skippedStepIds = emptyList(),
                currentStepIndex = 1
            )
            verify(routineDao).updateRun(expectedCompletedRun)

            // Step 2 skipped
            `when`(routineDao.getRunById(runId)).thenReturn(expectedCompletedRun)
            repository.recordStepSkipped(runId, step2Id, nextStepIndex = 2)
            val expectedSkippedRun = expectedCompletedRun.copy(
                completedStepIds = listOf(step1Id.toString()),
                skippedStepIds = listOf(step2Id.toString()),
                currentStepIndex = 2
            )
            verify(routineDao).updateRun(expectedSkippedRun)
        }
    }

    @Test
    fun testReorderSteps() {
        runBlocking {
            val routineId = UUID.randomUUID()
            val step1Id = UUID.randomUUID()
            val step2Id = UUID.randomUUID()

            val step1 = RoutineStep(id = step1Id, routineId = routineId, title = "Step 1", orderIndex = 0)
            val step2 = RoutineStep(id = step2Id, routineId = routineId, title = "Step 2", orderIndex = 1)

            `when`(routineDao.getStepsForRoutine(routineId)).thenReturn(listOf(step1, step2))

            repository.reorderSteps(routineId, listOf(step2Id, step1Id))

            verify(routineDao).updateSteps(
                listOf(
                    step2.copy(orderIndex = 0),
                    step1.copy(orderIndex = 1)
                )
            )
        }
    }
}

