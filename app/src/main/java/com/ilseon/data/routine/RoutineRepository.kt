package com.ilseon.data.routine

import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

interface RoutineRepository {
    // --- Routine CRUD ---
    fun observeActiveRoutines(): Flow<List<RoutineWithSteps>>
    fun observeActiveRoutinesByContext(contextId: UUID): Flow<List<RoutineWithSteps>>
    fun observeArchivedRoutines(): Flow<List<RoutineWithSteps>>
    fun observeRoutineWithSteps(routineId: UUID): Flow<RoutineWithSteps?>

    suspend fun getRoutineById(routineId: UUID): Routine?
    suspend fun getRoutineWithSteps(routineId: UUID): RoutineWithSteps?

    suspend fun createRoutine(routine: Routine, steps: List<RoutineStep>): UUID
    suspend fun updateRoutine(routine: Routine)
    suspend fun archiveRoutine(routineId: UUID, isArchived: Boolean = true)
    suspend fun deleteRoutine(routine: Routine)

    // --- Routine Steps ---
    suspend fun addStep(step: RoutineStep): UUID
    suspend fun updateStep(step: RoutineStep)
    suspend fun deleteStep(stepId: UUID)
    suspend fun reorderSteps(routineId: UUID, orderedStepIds: List<UUID>)

    // --- Execution Session / Auto-Reset Logic ---
    fun calculateSessionDate(currentTimeMillis: Long, cutoffHour: Int = 4): Long
    suspend fun getOrCreateTodayRun(routineId: UUID, currentTimeMillis: Long = System.currentTimeMillis()): RoutineRun
    fun observeTodayRun(routineId: UUID, currentTimeMillis: Long = System.currentTimeMillis()): Flow<RoutineRun?>

    suspend fun recordStepCompleted(runId: UUID, stepId: UUID, nextStepIndex: Int)
    suspend fun recordStepSkipped(runId: UUID, stepId: UUID, nextStepIndex: Int)
    suspend fun completeRun(runId: UUID)
    suspend fun resetTodayRun(routineId: UUID, currentTimeMillis: Long = System.currentTimeMillis())
}

class RoutineRepositoryImpl(
    private val routineDao: RoutineDao
) : RoutineRepository {

    override fun observeActiveRoutines(): Flow<List<RoutineWithSteps>> =
        routineDao.observeActiveRoutinesWithSteps()

    override fun observeActiveRoutinesByContext(contextId: UUID): Flow<List<RoutineWithSteps>> =
        routineDao.observeActiveRoutinesByContext(contextId)

    override fun observeArchivedRoutines(): Flow<List<RoutineWithSteps>> =
        routineDao.observeArchivedRoutinesWithSteps()

    override fun observeRoutineWithSteps(routineId: UUID): Flow<RoutineWithSteps?> =
        routineDao.observeRoutineWithStepsById(routineId)

    override suspend fun getRoutineById(routineId: UUID): Routine? =
        routineDao.getRoutineById(routineId)

    override suspend fun getRoutineWithSteps(routineId: UUID): RoutineWithSteps? =
        routineDao.getRoutineWithStepsById(routineId)

    override suspend fun createRoutine(routine: Routine, steps: List<RoutineStep>): UUID {
        routineDao.insertRoutine(routine)
        if (steps.isNotEmpty()) {
            val stepsWithParent = steps.mapIndexed { index, step ->
                step.copy(routineId = routine.id, orderIndex = index)
            }
            routineDao.insertSteps(stepsWithParent)
        }
        return routine.id
    }

    override suspend fun updateRoutine(routine: Routine) {
        routineDao.updateRoutine(routine)
    }

    override suspend fun archiveRoutine(routineId: UUID, isArchived: Boolean) {
        val routine = routineDao.getRoutineById(routineId) ?: return
        routineDao.updateRoutine(routine.copy(isArchived = isArchived))
    }

    override suspend fun deleteRoutine(routine: Routine) {
        routineDao.deleteRoutine(routine)
    }

    override suspend fun addStep(step: RoutineStep): UUID {
        val existingSteps = routineDao.getStepsForRoutine(step.routineId)
        val newOrderIndex = if (existingSteps.isEmpty()) 0 else (existingSteps.maxOf { it.orderIndex } + 1)
        val stepToInsert = step.copy(orderIndex = newOrderIndex)
        routineDao.insertStep(stepToInsert)
        return stepToInsert.id
    }

    override suspend fun updateStep(step: RoutineStep) {
        routineDao.updateStep(step)
    }

    override suspend fun deleteStep(stepId: UUID) {
        routineDao.deleteStepById(stepId)
    }

    override suspend fun reorderSteps(routineId: UUID, orderedStepIds: List<UUID>) {
        val existing = routineDao.getStepsForRoutine(routineId).associateBy { it.id }
        val updated = orderedStepIds.mapIndexedNotNull { index, id ->
            existing[id]?.copy(orderIndex = index)
        }
        if (updated.isNotEmpty()) {
            routineDao.updateSteps(updated)
        }
    }

    /**
     * Calculates the start of the logical day (in millis) anchored around [cutoffHour] (e.g. 04:00 AM).
     *
     * Example with cutoffHour = 4:
     * - Tuesday 02:30 AM -> belongs to the cycle starting Monday 04:00 AM.
     * - Tuesday 04:00 AM -> belongs to the cycle starting Tuesday 04:00 AM.
     * - Tuesday 23:50 PM -> belongs to the cycle starting Tuesday 04:00 AM.
     */
    override fun calculateSessionDate(currentTimeMillis: Long, cutoffHour: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        if (currentHour < cutoffHour) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, cutoffHour)
        return calendar.timeInMillis
    }

    override suspend fun getOrCreateTodayRun(routineId: UUID, currentTimeMillis: Long): RoutineRun {
        val routine = routineDao.getRoutineById(routineId)
        val cutoff = routine?.resetCutoffHour ?: 4
        val sessionDate = calculateSessionDate(currentTimeMillis, cutoff)

        val existing = routineDao.getRunForRoutineAndSessionDate(routineId, sessionDate)
        if (existing != null) {
            return existing
        }

        val newRun = RoutineRun(
            routineId = routineId,
            sessionDate = sessionDate,
            startedAt = currentTimeMillis
        )
        routineDao.insertRun(newRun)
        return newRun
    }

    override fun observeTodayRun(routineId: UUID, currentTimeMillis: Long): Flow<RoutineRun?> {
        val cutoff = 4 // standard default cutoff
        val sessionDate = calculateSessionDate(currentTimeMillis, cutoff)
        return routineDao.observeRunForRoutineAndSessionDate(routineId, sessionDate)
    }

    override suspend fun recordStepCompleted(runId: UUID, stepId: UUID, nextStepIndex: Int) {
        val run = routineDao.getRunById(runId) ?: return
        val stepIdStr = stepId.toString()
        val updatedCompleted = if (!run.completedStepIds.contains(stepIdStr)) {
            run.completedStepIds + stepIdStr
        } else {
            run.completedStepIds
        }
        val updatedSkipped = run.skippedStepIds - stepIdStr

        routineDao.updateRun(
            run.copy(
                completedStepIds = updatedCompleted,
                skippedStepIds = updatedSkipped,
                currentStepIndex = nextStepIndex
            )
        )
    }

    override suspend fun recordStepSkipped(runId: UUID, stepId: UUID, nextStepIndex: Int) {
        val run = routineDao.getRunById(runId) ?: return
        val stepIdStr = stepId.toString()
        val updatedSkipped = if (!run.skippedStepIds.contains(stepIdStr)) {
            run.skippedStepIds + stepIdStr
        } else {
            run.skippedStepIds
        }
        val updatedCompleted = run.completedStepIds - stepIdStr

        routineDao.updateRun(
            run.copy(
                completedStepIds = updatedCompleted,
                skippedStepIds = updatedSkipped,
                currentStepIndex = nextStepIndex
            )
        )
    }

    override suspend fun completeRun(runId: UUID) {
        val run = routineDao.getRunById(runId) ?: return
        routineDao.updateRun(
            run.copy(
                completedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun resetTodayRun(routineId: UUID, currentTimeMillis: Long) {
        val routine = routineDao.getRoutineById(routineId)
        val cutoff = routine?.resetCutoffHour ?: 4
        val sessionDate = calculateSessionDate(currentTimeMillis, cutoff)
        val existing = routineDao.getRunForRoutineAndSessionDate(routineId, sessionDate) ?: return

        routineDao.updateRun(
            existing.copy(
                completedAt = null,
                currentStepIndex = 0,
                completedStepIds = emptyList(),
                skippedStepIds = emptyList(),
                isAbandoned = false
            )
        )
    }
}
