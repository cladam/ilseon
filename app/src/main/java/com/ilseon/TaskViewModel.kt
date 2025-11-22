package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.DayOfWeek
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.SettingsRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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

sealed class PostCompletionAction {
    object Idle : PostCompletionAction()
    object GoToDashboard : PostCompletionAction()
    data class ActivateNextTask(val task: Task) : PostCompletionAction()
}

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val hapticManager: HapticManager,
    private val soundManager: SoundManager,
    private val notificationService: NotificationService,
    private val reminderManager: ReminderManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _taskForReflection = MutableStateFlow<Task?>(null)
    val taskForReflection: StateFlow<Task?> = _taskForReflection.asStateFlow()

    private val _postCompletionAction = MutableStateFlow<PostCompletionAction>(PostCompletionAction.Idle)
    val postCompletionAction: StateFlow<PostCompletionAction> = _postCompletionAction.asStateFlow()

    fun onShowReflectionDialog(taskId: UUID) {
        viewModelScope.launch {
            _taskForReflection.value = taskRepository.getTaskById(taskId)
        }
    }

    fun onReflectionDialogDismiss() {
        _taskForReflection.value = null
    }

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

    val completionStreak: StateFlow<Int> = taskRepository.getCompletionStreak()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val notifiedFocusBlocksStartingSoon = mutableSetOf<UUID>()
    private val notifiedFocusBlocksEndingSoon = mutableSetOf<UUID>()
    private val notifiedTasksStartingSoon = mutableSetOf<UUID>()
    private val taskPauseTimes = ConcurrentHashMap<UUID, Long>()
    private val taskNagTimes = ConcurrentHashMap<UUID, Long>()

    // State for tracking focus block notifications
    private var hasSeenFirstFocusBlock = false
    private var lastNotifiedFocusBlockId: UUID? = null

    companion object {
        val NAGGING_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(30)
    }

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

                if (currentId != null && !hasSeenFirstFocusBlock) {
                    hasSeenFirstFocusBlock = true
                    lastNotifiedFocusBlockId = currentId
                    return@collect
                }

                if (currentId != lastNotifiedFocusBlockId) {
                    focusBlock?.let {
                        val context = taskRepository.getContextById(it.contextId)
                        context?.let {
                            notificationService.sendFocusBlockStartedNotification(it.name)
                            hapticManager.performSuccess()
                        }
                    }
                    lastNotifiedFocusBlockId = currentId
                }
            }
        }
    }

    private suspend fun checkTasks() {
        val now = System.currentTimeMillis()
        val naggingEnabled = settingsRepository.naggingNotificationsEnabled.first()

        tasks.value.forEach { task ->
            val shouldStart = task.startTime != null && task.startTime <= now && (task.endTime == null || now < task.endTime)
            if ((task.timerState == TimerState.NotStarted || task.timerState == TimerState.Finished) && shouldStart) {
                startTask(task)
            }

            if (task.timerState == TimerState.NotStarted && task.startTime != null) {
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

            if (naggingEnabled && task.priority == TaskPriority.High && !task.isComplete) {
                val isOverdue = task.dueTime?.let { it < now } ?: false
                val isUnscheduledAndOld = task.schedulingType == SchedulingType.None && (now - task.createdAt > NAGGING_INTERVAL_MILLIS)

                if (isOverdue || isUnscheduledAndOld) {
                    val lastNagTime = taskNagTimes[task.id]
                    if (lastNagTime == null || (now - lastNagTime > NAGGING_INTERVAL_MILLIS)) {
                        notificationService.sendNaggingNotification(task)
                        hapticManager.performNudge()
                        taskNagTimes[task.id] = now
                    }
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
                if (task.dueTime != null) {
                    val newRemainingTime = (task.dueTime - now) / 1000
                    if (newRemainingTime > 0) {
                        val updatedTask = task.copy(
                            remainingTimeInSeconds = newRemainingTime,
                            timerState = TimerState.Running
                        )
                        taskRepository.updateTask(updatedTask)
                        reminderManager.rescheduleReminders(updatedTask)
                    } else {
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
        notificationService.sendTaskFinishedNotification(task)
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
        durationInMinutes: Int?,
        isRecurring: Boolean,
        recurrenceDays: Set<DayOfWeek>
    ) {
        viewModelScope.launch {
            if (title.isNotBlank() && contextId != null) {
                var startTime: Long? = null
                var endTime: Long? = null
                var duration: Int? = durationInMinutes
                var timerState = TimerState.NotStarted
                var schedulingType: SchedulingType
                var dueTime: Long? = null
                var recurrenceDaysString: String? = null

                if (isRecurring) {
                    recurrenceDaysString = recurrenceDays.sorted().joinToString(",") { it.name }
                }

                if (startTimeStr.isNotBlank() && endTimeStr.isNotBlank()) {
                    schedulingType = SchedulingType.TimeBlock
                    val (st, et, dur) = parseTimeAndCalculateDuration(startTimeStr, endTimeStr)
                    startTime = st
                    endTime = et
                    dueTime = et
                    duration = dur
                } else if (durationInMinutes != null) {
                    schedulingType = SchedulingType.Duration
                    if (isRecurring && startTimeStr.isNotBlank()) {
                        val (st, et) = parseStartTimeAndCalculateEndTime(startTimeStr, durationInMinutes)
                        startTime = st
                        endTime = et
                        dueTime = et
                    }
                } else {
                    schedulingType = SchedulingType.None
                    if (isRecurring && startTimeStr.isNotBlank()) {
                        val (st, _) = parseStartTimeAndCalculateEndTime(startTimeStr, 0)
                        startTime = st
                        dueTime = st
                    }
                }

                val newId = UUID.randomUUID()
                val newTask = Task(
                    id = newId,
                    title = title,
                    description = description,
                    contextId = contextId,
                    priority = priority,
                    schedulingType = schedulingType,
                    startTime = startTime,
                    endTime = endTime,
                    dueTime = dueTime,
                    totalTimeInMinutes = duration,
                    timerState = timerState,
                    isRecurring = isRecurring,
                    recurrenceDays = recurrenceDaysString,
                    seriesId = if (isRecurring) newId else null
                )
                taskRepository.insertTask(newTask)
                reminderManager.rescheduleReminders(newTask)
            }
        }
    }

    private fun parseStartTimeAndCalculateEndTime(startTimeStr: String, durationInMinutes: Int): Pair<Long?, Long?> {
        if (startTimeStr.isBlank()) {
            return Pair(null, null)
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        try {
            val today = Calendar.getInstance()
            val startCal = Calendar.getInstance()
            startCal.time = timeFormat.parse(startTimeStr) ?: return Pair(null, null)
            startCal.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))

            val startTime = startCal.timeInMillis
            val endTime = startTime + TimeUnit.MINUTES.toMillis(durationInMinutes.toLong())

            return Pair(startTime, endTime)
        } catch (e: Exception) {
            return Pair(null, null)
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
                completionReflection = reflectionToSave,
                timerState = TimerState.Finished
            )
            taskRepository.updateTask(updatedTask)
            reminderManager.cancelReminder(updatedTask)
            prepareForNextTaskTransition(updatedTask)
        }
    }

    private fun prepareForNextTaskTransition(completedTask: Task) {
        viewModelScope.launch {
            val currentTasks = tasks.value
            val remainingTasks = currentTasks.filterNot { it.id == completedTask.id }

            if (remainingTasks.isEmpty()) {
                _postCompletionAction.value = PostCompletionAction.GoToDashboard
            } else {
                _postCompletionAction.value = PostCompletionAction.ActivateNextTask(remainingTasks.first())
            }
        }
    }
    
    fun postCompletionActionHandled() {
        _postCompletionAction.value = PostCompletionAction.Idle
    }

    fun startNextTask(task: Task) {
        viewModelScope.launch {
            startTaskTimer(task)
            postCompletionActionHandled()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
            if (task.dueTime != null || task.startTime != null) {
                reminderManager.rescheduleReminders(task)
            }
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

            val pauseStartTime = taskPauseTimes.remove(task.id)
            if (task.timerState == TimerState.Paused && pauseStartTime != null) {
                val pauseDuration = now - pauseStartTime
                val newDueTime = (task.dueTime ?: now) + pauseDuration
                updatedTask = updatedTask.copy(dueTime = newDueTime)
            }

            if (task.schedulingType == SchedulingType.Duration && task.dueTime == null) {
                val newDueTime = now + (task.remainingTimeInSeconds * 1000)
                updatedTask = updatedTask.copy(dueTime = newDueTime)
            }

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

    fun pauseTaskTimer(task: Task) {
        viewModelScope.launch {
            if (task.timerState == TimerState.Running) {
                val now = System.currentTimeMillis()
                taskPauseTimes[task.id] = now

                var updatedTask = task.copy(timerState = TimerState.Paused)

                if (task.schedulingType == SchedulingType.Duration) {
                    val elapsed = now - (task.timerStartTime ?: now)
                    val newRemaining = task.remainingTimeInSeconds - (elapsed / 1000)
                    updatedTask =
                        updatedTask.copy(remainingTimeInSeconds = max(0, newRemaining))
                }
                taskRepository.updateTask(updatedTask)
                reminderManager.cancelReminder(updatedTask)
            }
        }
    }
}
