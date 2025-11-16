package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.task.TimerState
import com.ilseon.service.HapticManager
import com.ilseon.service.NotificationService
import com.ilseon.service.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val hapticManager: HapticManager,
    private val soundManager: SoundManager,
    private val notificationService: NotificationService
) : ViewModel() {

    val activeFocusBlock: StateFlow<FocusBlock?> = taskRepository.getActiveFocusBlock()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val tasks: StateFlow<List<Task>> = taskRepository.getIncompleteTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val notifiedFocusBlocksStartingSoon = mutableSetOf<UUID>()
    private val notifiedFocusBlocksEndingSoon = mutableSetOf<UUID>()
    private val notifiedTasksStartingSoon = mutableSetOf<UUID>()

    init {
        viewModelScope.launch {
            restoreRunningTasksState()
            monitorFocusBlockChanges()
            while (isActive) {
                checkTasks()
                checkFocusBlocks()
                delay(1000) // Check every second
            }
        }
    }

    fun isTaskOverdue(task: Task): Boolean {
        if (task.isComplete) return false
        val due = task.dueTime ?: task.endTime ?: return false
        return System.currentTimeMillis() > due
    }

    private fun monitorFocusBlockChanges() {
        viewModelScope.launch {
            activeFocusBlock.collect { focusBlock ->
                focusBlock?.let {
                    val context = taskRepository.getContextById(it.contextId)
                    context?.let {
                        notificationService.sendFocusBlockStartedNotification(it.name)
                        hapticManager.performSuccess()
                    }
                }
            }
        }
    }

    private suspend fun checkTasks() {
        val now = System.currentTimeMillis()
        tasks.value.forEach { task ->
            if (task.timerState == TimerState.NotStarted && task.startTime != null) {
                if (task.startTime <= now && (task.endTime == null || now < task.endTime)) {
                    startTask(task)
                }

                val fiveMinutesInMillis = 5 * 60 * 1000
                if (task.startTime > now && task.startTime - now < fiveMinutesInMillis && !notifiedTasksStartingSoon.contains(task.id)) {
                    notificationService.sendTaskStartingSoonNotification(task.title, 5)
                    hapticManager.performNudge()
                    notifiedTasksStartingSoon.add(task.id)
                }
            }
        }
    }

    private suspend fun checkFocusBlocks() {
        val now = LocalTime.now()
        val allFocusBlocks = taskRepository.getAllFocusBlocks()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        allFocusBlocks.forEach { focusBlock ->
            val context = taskRepository.getContextById(focusBlock.contextId)
            context?.let {
                val startTime = LocalTime.parse(focusBlock.startTime, formatter)
                val endTime = LocalTime.parse(focusBlock.endTime, formatter)
                val fiveMinutes = 5L

                // Starting soon notification
                if (now.isBefore(startTime) && now.plusMinutes(fiveMinutes).isAfter(startTime) && !notifiedFocusBlocksStartingSoon.contains(focusBlock.id)) {
                    notificationService.sendFocusBlockStartingSoonNotification(it.name, 5)
                    hapticManager.performNudge()
                    notifiedFocusBlocksStartingSoon.add(focusBlock.id)
                }

                // Ending soon notification
                if (now.isBefore(endTime) && now.plusMinutes(fiveMinutes).isAfter(endTime) && !notifiedFocusBlocksEndingSoon.contains(focusBlock.id)) {
                    notificationService.sendFocusBlockEndingSoonNotification(it.name, 5)
                    hapticManager.performNudge()
                    notifiedFocusBlocksEndingSoon.add(focusBlock.id)
                }
            }
        }
    }

    private fun restoreRunningTasksState() {
        viewModelScope.launch {
            val runningTasks = taskRepository.getRunningTasks()
            val now = System.currentTimeMillis()
            runningTasks.forEach { task ->
                if (task.timerStartTime != null) {
                    val elapsedTimeInSeconds = (now - task.timerStartTime) / 1000
                    val newRemainingTime = task.remainingTimeInSeconds - elapsedTimeInSeconds
                    if (newRemainingTime > 0) {
                        taskRepository.updateTask(
                            task.copy(
                                remainingTimeInSeconds = newRemainingTime,
                                timerState = TimerState.Running // Keep it running
                            )
                        )
                    } else {
                        // Timer finished while app was closed
                        onTaskTimerFinished(task)
                        taskRepository.updateTask(
                            task.copy(
                                remainingTimeInSeconds = 0,
                                timerState = TimerState.NotStarted,
                                isComplete = true,
                                completedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }

    fun startTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(timerState = TimerState.Running))
        }
    }

    fun onTaskTimerFinished(task: Task) {
        hapticManager.performAlert()
        notificationService.sendTaskFinishedNotification(task.title)
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    fun addTask(
        title: String,
        description: String?,
        contextId: UUID?,
        priority: TaskPriority,
        startTimeStr: String,
        endTimeStr: String,
        durationInMinutes: Int?
    ) {
        viewModelScope.launch {
            if (title.isNotBlank() && contextId != null) {
                var startTime: Long? = null
                var endTime: Long? = null
                var duration: Int? = null
                var timerState = TimerState.NotStarted
                var schedulingType = SchedulingType.None
                var dueTime: Long? = null

                if (startTimeStr.isNotBlank() && endTimeStr.isNotBlank()) {
                    val (st, et, dur) = parseTimeAndCalculateDuration(startTimeStr, endTimeStr)
                    startTime = st
                    endTime = et
                    dueTime = et
                    duration = dur
                    schedulingType = SchedulingType.TimeBlock
                } else if (durationInMinutes != null) {
                    duration = durationInMinutes
                    schedulingType = SchedulingType.Duration
                }

                taskRepository.insertTask(
                    Task(
                        title = title,
                        description = description,
                        contextId = contextId,
                        priority = priority,
                        schedulingType = schedulingType,
                        startTime = startTime,
                        endTime = endTime,
                        dueTime = dueTime,
                        totalTimeInMinutes = duration,
                        timerState = timerState
                    )
                )
            }
        }
    }

    private fun parseTimeAndCalculateDuration(
        startTimeStr: String,
        endTimeStr: String
    ): Triple<Long?, Long?, Int?> {
        if (startTimeStr.isBlank() || endTimeStr.isBlank()) {
            return Triple(null, null, null)
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        try {
            val today = Calendar.getInstance()

            val startCal = Calendar.getInstance()
            startCal.time = timeFormat.parse(startTimeStr) ?: return Triple(null, null, null)
            startCal.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))

            val endCal = Calendar.getInstance()
            endCal.time = timeFormat.parse(endTimeStr) ?: return Triple(null, null, null)
            endCal.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))

            if (endCal.timeInMillis <= startCal.timeInMillis) {
                return Triple(null, null, null)
            }

            val startTime = startCal.timeInMillis
            val endTime = endCal.timeInMillis
            val duration = ((endTime - startTime) / (1000 * 60)).toInt()

            return Triple(startTime, endTime, duration)
        } catch (e: Exception) {
            return Triple(null, null, null)
        }
    }

    fun completeTask(task: Task, completionReflection: String) {
        viewModelScope.launch {
            hapticManager.performSuccess()
            taskRepository.updateTask(
                task.copy(
                    isComplete = true,
                    completedAt = System.currentTimeMillis(),
                    completionReflection = completionReflection
                )
            )
        }
    }

    fun startTaskTimer(task: Task) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var dueTime = task.dueTime
            if (task.schedulingType == SchedulingType.Duration && dueTime == null) {
                dueTime = now + (task.remainingTimeInSeconds * 1000)
            }

            taskRepository.updateTask(
                task.copy(
                    timerState = TimerState.Running,
                    timerStartTime = now,
                    dueTime = dueTime
                )
            )
        }
    }

    fun pauseTaskTimer(task: Task) {
        viewModelScope.launch {
            if (task.timerState == TimerState.Running) {
                val now = System.currentTimeMillis()
                val elapsed = now - (task.timerStartTime ?: now)
                val newRemaining = task.remainingTimeInSeconds - (elapsed / 1000)
                var newDueTime = task.dueTime
                if (task.schedulingType == SchedulingType.Duration && newDueTime != null) {
                    newDueTime += elapsed
                }

                taskRepository.updateTask(
                    task.copy(
                        timerState = TimerState.Paused,
                        remainingTimeInSeconds = max(0, newRemaining),
                        dueTime = newDueTime
                    )
                )
            }
        }
    }
}