package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.task.TimerState
import com.ilseon.notifications.ReminderManager
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val hapticManager: HapticManager,
    private val soundManager: SoundManager,
    private val notificationService: NotificationService,
    private val reminderManager: ReminderManager
) : ViewModel() {

    val activeFocusBlock: StateFlow<FocusBlock?> = taskRepository.getActiveFocusBlock()
        .distinctUntilChanged()
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
    private val taskPauseTimes = ConcurrentHashMap<UUID, Long>()

    // State for tracking focus block notifications
    private var hasSeenFirstFocusBlock = false
    private var lastNotifiedFocusBlockId: UUID? = null

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
        // A task can only be overdue if it's currently running.
        if (task.isComplete || task.timerState != TimerState.Running) return false
        val due = task.dueTime ?: task.endTime ?: return false
        return System.currentTimeMillis() > due
    }

    fun isTaskVisuallyOverdue(task: Task): Boolean {
        if (task.isComplete) return false
        // Only time-blocked tasks can be visually overdue while still running.
        if (task.schedulingType != SchedulingType.TimeBlock) return false
        val endTime = task.endTime ?: return false
        // It's visually overdue if the current time is past the planned end time.
        return System.currentTimeMillis() > endTime
    }

    private fun monitorFocusBlockChanges() {
        viewModelScope.launch {
            activeFocusBlock.collect { focusBlock ->
                val currentId = focusBlock?.id

                // If it's the first non-null block we've seen since the app started,
                // treat it as the initial state and don't notify. This prevents
                // notifications for already-active blocks on app launch.
                if (currentId != null && !hasSeenFirstFocusBlock) {
                    hasSeenFirstFocusBlock = true
                    lastNotifiedFocusBlockId = currentId
                    return@collect
                }

                // If the state changes *after* the initial state has been seen...
                if (currentId != lastNotifiedFocusBlockId) {
                    // ...and the new state is a valid block (i.e., a block has started)...
                    focusBlock?.let {
                        //...then send the notification.
                        val context = taskRepository.getContextById(it.contextId)
                        context?.let {
                            notificationService.sendFocusBlockStartedNotification(it.name)
                            hapticManager.performSuccess()
                        }
                    }
                    // Update the state for the next change.
                    lastNotifiedFocusBlockId = currentId
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
                    val minutesUntilStart = TimeUnit.MILLISECONDS.toMinutes(task.startTime - now) + 1
                    notificationService.sendTaskStartingSoonNotification(
                        task.title,
                        minutesUntilStart.toInt()
                    )
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
                if (now.isBefore(startTime) && now.plusMinutes(fiveMinutes)
                        .isAfter(startTime) && !notifiedFocusBlocksStartingSoon.contains(focusBlock.id)
                ) {
                    val minutesUntilStart = java.time.Duration.between(now, startTime).toMinutes() + 1
                    notificationService.sendFocusBlockStartingSoonNotification(
                        it.name,
                        minutesUntilStart.toInt()
                    )
                    hapticManager.performNudge()
                    notifiedFocusBlocksStartingSoon.add(focusBlock.id)
                }

                // Ending soon notification
                if (now.isBefore(endTime) && now.plusMinutes(fiveMinutes)
                        .isAfter(endTime) && !notifiedFocusBlocksEndingSoon.contains(focusBlock.id)
                ) {
                    val minutesUntilEnd = java.time.Duration.between(now, endTime).toMinutes() + 1
                    notificationService.sendFocusBlockEndingSoonNotification(
                        it.name,
                        minutesUntilEnd.toInt()
                    )
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
                        val updatedTask = task.copy(
                            remainingTimeInSeconds = newRemainingTime,
                            timerState = TimerState.Running // Keep it running
                        )
                        taskRepository.updateTask(updatedTask)
                        reminderManager.rescheduleReminders(updatedTask)
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
            val now = System.currentTimeMillis()
            var updatedTask = task.copy(
                timerState = TimerState.Running,
                timerStartTime = now
            )

            if (task.schedulingType == SchedulingType.TimeBlock) {
                val dueTime = updatedTask.dueTime ?: task.dueTime ?: task.endTime
                dueTime?.let {
                    val newRemaining = (it - now) / 1000
                    updatedTask =
                        updatedTask.copy(remainingTimeInSeconds = max(0, newRemaining))
                }
            }
            taskRepository.updateTask(updatedTask)
            reminderManager.rescheduleReminders(updatedTask)
        }
    }

    fun onTaskTimerFinished(task: Task) {
        hapticManager.performAlert()
        notificationService.sendTaskFinishedNotification(task.title)
        reminderManager.cancelReminder(task)
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

                val newTask = Task(
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
                taskRepository.insertTask(newTask)
                reminderManager.rescheduleReminders(newTask)
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
            val reflectionToSave = if (completionReflection.isBlank()) null else completionReflection
            val updatedTask = task.copy(
                isComplete = true,
                completedAt = System.currentTimeMillis(),
                completionReflection = reflectionToSave
            )
            taskRepository.updateTask(updatedTask)
            reminderManager.cancelReminder(updatedTask)
        }
    }

    fun startTaskTimer(task: Task) {
        viewModelScope.launch {
            if (task.schedulingType == SchedulingType.TimeBlock && task.timerState == TimerState.NotStarted) {
                return@launch
            }

            val now = System.currentTimeMillis()
            var updatedTask = task.copy(
                timerState = TimerState.Running,
                timerStartTime = now
            )

            // Handle resuming from pause
            val pauseStartTime = taskPauseTimes.remove(task.id)
            if (task.timerState == TimerState.Paused && pauseStartTime != null) {
                val pauseDuration = now - pauseStartTime
                val newDueTime = (task.dueTime ?: now) + pauseDuration
                updatedTask = updatedTask.copy(dueTime = newDueTime)
            }

            // Handle first start of a duration task
            if (task.schedulingType == SchedulingType.Duration && task.dueTime == null) {
                val newDueTime = now + (task.remainingTimeInSeconds * 1000)
                updatedTask = updatedTask.copy(dueTime = newDueTime)
            }

            // For TimeBlock tasks, remaining time is always calculated from its deadline.
            if (task.schedulingType == SchedulingType.TimeBlock) {
                // Use the potentially updated dueTime
                val dueTime = updatedTask.dueTime ?: task.dueTime ?: task.endTime
                dueTime?.let {
                    val newRemaining = (it - now) / 1000
                    updatedTask =
                        updatedTask.copy(remainingTimeInSeconds = max(0, newRemaining))
                }
            }

            taskRepository.updateTask(updatedTask)
            reminderManager.rescheduleReminders(updatedTask)
        }
    }

    fun pauseTaskTimer(task: Task) {
        viewModelScope.launch {
            if (task.timerState == TimerState.Running) {
                val now = System.currentTimeMillis()
                taskPauseTimes[task.id] = now // Record pause time

                var updatedTask = task.copy(timerState = TimerState.Paused)

                if (task.schedulingType == SchedulingType.Duration) {
                    // For duration tasks, we must persist the new remaining time.
                    val elapsed = now - (task.timerStartTime ?: now)
                    val newRemaining = task.remainingTimeInSeconds - (elapsed / 1000)
                    updatedTask =
                        updatedTask.copy(remainingTimeInSeconds = max(0, newRemaining))
                }
                // For TimeBlock tasks, we only need to pause the state.
                // The remaining time will be recalculated on resume based on the new dueTime.
                taskRepository.updateTask(updatedTask)
                reminderManager.cancelReminder(updatedTask)
            }
        }
    }
}