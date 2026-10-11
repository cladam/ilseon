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
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val contextRepository: TaskContextRepository
) : ViewModel() {

    val activeRoutines: StateFlow<List<RoutineWithSteps>> =
        routineRepository.observeActiveRoutines()
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
     * Determines which routine is currently enabled / ready for the dashboard.
     * Evaluates focus block context and time window (e.g. starts at 07:00).
     */
    fun getDashboardReadyRoutine(
        routines: List<RoutineWithSteps>,
        activeFocusBlock: FocusBlock?,
        nowCalendar: Calendar = Calendar.getInstance()
    ): RoutineWithSteps? {
        val currentHour = nowCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = nowCalendar.get(Calendar.MINUTE)
        val currentMinutesOfDay = currentHour * 60 + currentMinute

        return routines.firstOrNull { routineWithSteps ->
            val routine = routineWithSteps.routine

            // Context filter: if a focus block is active, only show routines belonging to it.
            // If no focus block is active, all routines are eligible.
            val contextMatches = activeFocusBlock == null || routine.contextId == activeFocusBlock.contextId
            if (!contextMatches) return@firstOrNull false

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

            // If startHour is specified, the routine is only enabled once the start time has arrived.
            // If no start time is specified, it is enabled all day.
            val isTimeEligible = when {
                startMinutesOfDay != null && endMinutesOfDay != null ->
                    currentMinutesOfDay in startMinutesOfDay..endMinutesOfDay
                startMinutesOfDay != null ->
                    currentMinutesOfDay >= startMinutesOfDay
                else -> true
            }

            isTimeEligible
        }
    }

    fun startRoutine(routineWithSteps: RoutineWithSteps) {
        viewModelScope.launch {
            val run = routineRepository.getOrCreateTodayRun(routineWithSteps.routine.id)
            _runningRoutine.value = routineWithSteps
            _currentRun.value = run
            _runnerUiState.value = RoutineRunnerUiState.FullOverlay
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
        viewModelScope.launch {
            routineRepository.recordStepCompleted(run.id, step.id, nextIndex)
            val updatedRun = run.copy(
                currentStepIndex = nextIndex,
                completedStepIds = run.completedStepIds + step.id.toString()
            )
            _currentRun.value = updatedRun
        }
    }

    fun onStepSkipped(step: RoutineStep, nextIndex: Int) {
        val run = _currentRun.value ?: return
        viewModelScope.launch {
            routineRepository.recordStepSkipped(run.id, step.id, nextIndex)
            val updatedRun = run.copy(
                currentStepIndex = nextIndex,
                skippedStepIds = run.skippedStepIds + step.id.toString()
            )
            _currentRun.value = updatedRun
        }
    }

    fun finishRoutine() {
        val run = _currentRun.value
        if (run != null) {
            viewModelScope.launch {
                routineRepository.completeRun(run.id)
            }
        }
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
