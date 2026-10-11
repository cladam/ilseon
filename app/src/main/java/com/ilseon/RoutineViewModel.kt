package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.routine.Routine
import com.ilseon.data.routine.RoutineRepository
import com.ilseon.data.routine.RoutineRun
import com.ilseon.data.routine.RoutineStep
import com.ilseon.data.routine.RoutineStepDraft
import com.ilseon.data.routine.RoutineWithSteps
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextRepository
import com.ilseon.service.HapticManager
import com.ilseon.wear.WearDataSender
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

enum class RoutineRunnerUiState {
    Hidden,
    CollapsedBar,
    FullOverlay
}

@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val contextRepository: TaskContextRepository,
    private val hapticManager: HapticManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val activeRoutines: StateFlow<List<RoutineWithSteps>> =
        routineRepository.observeActiveRoutines()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val todayRuns: StateFlow<List<RoutineRun>> =
        routineRepository.observeAllRunsForToday()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val contexts: StateFlow<List<TaskContext>> =
        contextRepository.getContexts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Current active running routine session
    private val _runningRoutine = MutableStateFlow<RoutineWithSteps?>(null)
    val runningRoutine: StateFlow<RoutineWithSteps?> = _runningRoutine.asStateFlow()

    private val _currentRun = MutableStateFlow<RoutineRun?>(null)
    val currentRun: StateFlow<RoutineRun?> = _currentRun.asStateFlow()

    private val _runnerUiState = MutableStateFlow(RoutineRunnerUiState.Hidden)
    val runnerUiState: StateFlow<RoutineRunnerUiState> = _runnerUiState.asStateFlow()

    private val _isCreateRoutineDialogOpen = MutableStateFlow(false)
    val isCreateRoutineDialogOpen: StateFlow<Boolean> = _isCreateRoutineDialogOpen.asStateFlow()

    private val _editingRoutine = MutableStateFlow<RoutineWithSteps?>(null)
    val editingRoutine: StateFlow<RoutineWithSteps?> = _editingRoutine.asStateFlow()

    fun openCreateRoutineDialog() {
        _editingRoutine.value = null
        _isCreateRoutineDialogOpen.value = true
    }

    fun openEditRoutineDialog(routineWithSteps: RoutineWithSteps) {
        _editingRoutine.value = routineWithSteps
        _isCreateRoutineDialogOpen.value = true
    }

    fun closeCreateRoutineDialog() {
        _isCreateRoutineDialogOpen.value = false
        _editingRoutine.value = null
    }

    /**
     * Data structure holding today's routine dashboard partition:
     * - candidateRoutines: All uncompleted routines eligible for today's active block, sorted with in-progress first
     * - selectedHeroRoutine: The primary routine to show in the Hero Card (or null if all done)
     * - completedRoutines: Routines that have already been completed today in this cycle
     */
    data class DashboardRoutineState(
        val candidateRoutines: List<Pair<RoutineWithSteps, RoutineRun?>>,
        val selectedHeroRoutine: Pair<RoutineWithSteps, RoutineRun?>?,
        val completedRoutines: List<Pair<RoutineWithSteps, RoutineRun>>
    )

    // Optional manual cycle index if user switches between multiple routines in the same block
    private val _manualRoutineIndex = MutableStateFlow(0)
    val manualRoutineIndex: StateFlow<Int> = _manualRoutineIndex.asStateFlow()

    fun cycleNextRoutine(totalCandidates: Int) {
        if (totalCandidates > 1) {
            _manualRoutineIndex.value = (_manualRoutineIndex.value + 1) % totalCandidates
        }
    }

    /**
     * Determines which routine is currently enabled / ready for the dashboard.
     * Evaluates focus block context, time window (e.g. starts at 07:00),
     * and separates completed runs so they don't block uncompleted ones.
     */
    fun getDashboardRoutineState(
        routines: List<RoutineWithSteps>,
        todayRuns: List<RoutineRun>,
        activeFocusBlock: FocusBlock?,
        manualIndex: Int = 0,
        nowCalendar: Calendar = Calendar.getInstance()
    ): DashboardRoutineState {
        val currentHour = nowCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = nowCalendar.get(Calendar.MINUTE)
        val currentMinutesOfDay = currentHour * 60 + currentMinute
        val runMap = todayRuns.associateBy { it.routineId }

        val eligibleRoutines = routines.filter { routineWithSteps ->
            val routine = routineWithSteps.routine

            // Context filter: if a focus block is active, only show routines belonging to it.
            // If no focus block is active, all routines are eligible.
            val contextMatches = activeFocusBlock == null || routine.contextId == activeFocusBlock.contextId
            if (!contextMatches) return@filter false

            // Start time filter: e.g. enabled at 7:00 AM
            val startMinutesOfDay = if (routine.startHour != null && routine.startMinute != null) {
                routine.startHour * 60 + routine.startMinute
            } else {
                null
            }

            // End time filter: e.g. enabled until 10:00 AM
            val endMinutesOfDay = if (routine.endHour != null && routine.endMinute != null) {
                routine.endHour * 60 + routine.endMinute
            } else {
                null
            }

            val isTimeEligible = when {
                startMinutesOfDay != null && endMinutesOfDay != null ->
                    currentMinutesOfDay in startMinutesOfDay..endMinutesOfDay
                startMinutesOfDay != null ->
                    currentMinutesOfDay >= startMinutesOfDay
                else -> true
            }

            isTimeEligible
        }

        val completed = mutableListOf<Pair<RoutineWithSteps, RoutineRun>>()
        val uncompleted = mutableListOf<Pair<RoutineWithSteps, RoutineRun?>>()

        eligibleRoutines.forEach { routineWithSteps ->
            val run = runMap[routineWithSteps.routine.id]
            if (run?.completedAt != null) {
                completed.add(routineWithSteps to run)
            } else {
                uncompleted.add(routineWithSteps to run)
            }
        }

        // Sort uncompleted routines:
        // 1. In-progress first (currentStepIndex > 0)
        // 2. Specific start times first
        // 3. Display order / creation order
        val sortedUncompleted = uncompleted.sortedWith(
            compareByDescending<Pair<RoutineWithSteps, RoutineRun?>> { (_, run) -> (run?.currentStepIndex ?: 0) > 0 }
                .thenBy { (routineWithSteps, _) -> routineWithSteps.routine.startHour ?: Int.MAX_VALUE }
                .thenBy { (routineWithSteps, _) -> routineWithSteps.routine.displayOrder }
        )

        val selectedHero = if (sortedUncompleted.isNotEmpty()) {
            val safeIndex = manualIndex.coerceIn(0, sortedUncompleted.lastIndex)
            sortedUncompleted[safeIndex]
        } else {
            null
        }

        return DashboardRoutineState(
            candidateRoutines = sortedUncompleted,
            selectedHeroRoutine = selectedHero,
            completedRoutines = completed
        )
    }

    private fun syncRoutineToWear(routine: RoutineWithSteps?, run: RoutineRun?) {
        viewModelScope.launch {
            if (routine == null || run == null) {
                WearDataSender.sendRoutineState(
                    context = appContext,
                    routineTitle = null,
                    stepTitle = null,
                    stepIndex = 0,
                    totalSteps = 0,
                    targetDurationMins = null,
                    isActive = false,
                    isComplete = false
                )
                return@launch
            }
            val steps = routine.sortedSteps
            val currentIndex = run.currentStepIndex.coerceIn(0, steps.size)
            val isComplete = run.completedAt != null || currentIndex >= steps.size
            val currentStep = if (!isComplete && steps.isNotEmpty()) steps[currentIndex] else null

            WearDataSender.sendRoutineState(
                context = appContext,
                routineTitle = routine.routine.title,
                stepTitle = currentStep?.title ?: "Complete!",
                stepIndex = currentIndex + 1,
                totalSteps = steps.size,
                targetDurationMins = currentStep?.targetDurationMinutes,
                isActive = true,
                isComplete = isComplete
            )
        }
    }

    fun startRoutine(routineWithSteps: RoutineWithSteps) {
        viewModelScope.launch {
            val run = routineRepository.getOrCreateTodayRun(routineWithSteps.routine.id)
            _runningRoutine.value = routineWithSteps
            _currentRun.value = run
            _runnerUiState.value = RoutineRunnerUiState.FullOverlay
            syncRoutineToWear(routineWithSteps, run)
        }
    }

    fun minimizeRunner() {
        _runnerUiState.value = RoutineRunnerUiState.CollapsedBar
    }

    fun expandRunner() {
        _runnerUiState.value = RoutineRunnerUiState.FullOverlay
    }

    fun onStepCompleted(step: RoutineStep, nextIndex: Int) {
        val run = _currentRun.value ?: return
        val routine = _runningRoutine.value
        hapticManager.performNudge()
        viewModelScope.launch {
            routineRepository.recordStepCompleted(run.id, step.id, nextIndex)
            val updatedRun = run.copy(
                currentStepIndex = nextIndex,
                completedStepIds = run.completedStepIds + step.id.toString()
            )
            _currentRun.value = updatedRun
            syncRoutineToWear(routine, updatedRun)
        }
    }

    fun onStepSkipped(step: RoutineStep, nextIndex: Int) {
        val run = _currentRun.value ?: return
        val routine = _runningRoutine.value
        hapticManager.performNudge()
        viewModelScope.launch {
            routineRepository.recordStepSkipped(run.id, step.id, nextIndex)
            val updatedRun = run.copy(
                currentStepIndex = nextIndex,
                skippedStepIds = run.skippedStepIds + step.id.toString()
            )
            _currentRun.value = updatedRun
            syncRoutineToWear(routine, updatedRun)
        }
    }

    fun onStepPacingElapsed() {
        hapticManager.performWarning()
    }

    fun finishRoutine() {
        hapticManager.performSuccess()
        val run = _currentRun.value
        if (run != null) {
            viewModelScope.launch {
                routineRepository.completeRun(run.id)
            }
        }
        syncRoutineToWear(null, null)
        _runnerUiState.value = RoutineRunnerUiState.Hidden
        _runningRoutine.value = null
        _currentRun.value = null
    }

    fun saveRoutine(
        existingRoutineId: UUID?,
        title: String,
        description: String?,
        contextId: UUID,
        startHour: Int?,
        startMinute: Int?,
        endHour: Int?,
        endMinute: Int?,
        daysOfWeek: String?,
        stepDrafts: List<RoutineStepDraft>
    ) {
        viewModelScope.launch {
            if (existingRoutineId != null) {
                // Update routine
                val existing = routineRepository.getRoutineById(existingRoutineId) ?: return@launch
                val updatedRoutine = existing.copy(
                    title = title,
                    description = description,
                    contextId = contextId,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    daysOfWeek = daysOfWeek
                )
                routineRepository.updateRoutine(updatedRoutine)

                // Replace steps
                val currentWithSteps = routineRepository.getRoutineWithSteps(existingRoutineId)
                currentWithSteps?.steps?.forEach { step ->
                    routineRepository.deleteStep(step.id)
                }
                stepDrafts.forEachIndexed { index, draft ->
                    routineRepository.addStep(
                        RoutineStep(
                            routineId = existingRoutineId,
                            title = draft.title,
                            orderIndex = index,
                            targetDurationMinutes = draft.targetDurationMinutes
                        )
                    )
                }
            } else {
                // Create brand new routine
                val newRoutine = Routine(
                    title = title,
                    description = description,
                    contextId = contextId,
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute,
                    daysOfWeek = daysOfWeek
                )
                val newSteps = stepDrafts.mapIndexed { index, draft ->
                    RoutineStep(
                        routineId = newRoutine.id,
                        title = draft.title,
                        orderIndex = index,
                        targetDurationMinutes = draft.targetDurationMinutes
                    )
                }
                routineRepository.createRoutine(newRoutine, newSteps)
            }
            closeCreateRoutineDialog()
        }
    }

    fun archiveRoutine(routineId: UUID) {
        viewModelScope.launch {
            routineRepository.archiveRoutine(routineId, isArchived = true)
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            routineRepository.deleteRoutine(routine)
        }
    }
}
