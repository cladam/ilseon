package com.ilseon

import android.content.Context
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
import com.ilseon.util.UsageStatsReader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.compareTo
import kotlin.math.max
import kotlin.text.set
import kotlin.text.toInt
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest

sealed class PostCompletionAction {
    object Idle : PostCompletionAction()
    object GoToDashboard : PostCompletionAction()
    data class ActivateNextTask(val task: Task) : PostCompletionAction()
}

data class ReflectionData(val task: Task, val phonePickups: Int?)

@HiltViewModel
class TaskViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val hapticManager: HapticManager,
    private val soundManager: SoundManager,
    private val notificationService: NotificationService,
    private val reminderManager: ReminderManager,
    private val settingsRepository: SettingsRepository,
    private val isTest: Boolean
) : ViewModel() {

    private val usageStatsReader = UsageStatsReader(context)

    private val _taskForReflection = MutableStateFlow<ReflectionData?>(null)
    val taskForReflection: StateFlow<ReflectionData?> = _taskForReflection.asStateFlow()


    private val _postCompletionAction = MutableStateFlow<PostCompletionAction>(PostCompletionAction.Idle)
    val postCompletionAction: StateFlow<PostCompletionAction> = _postCompletionAction.asStateFlow()

    private val _subTasks = MutableStateFlow<List<Task>>(emptyList())
    val subTasks: StateFlow<List<Task>> = _subTasks.asStateFlow()

    private var subTaskJob: Job? = null

    fun onShowReflectionDialog(taskId: UUID) {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                var phonePickups: Int? = null
                if (usageStatsReader.hasUsageStatsPermission()) {
                    val endTime = System.currentTimeMillis() // Assuming completion is now
                    val startTime = if (task.schedulingType == SchedulingType.Duration && task.dueTime != null && task.totalTimeInMinutes != null) {
                        task.dueTime - (task.totalTimeInMinutes * 60 * 1000L)
                    } else {
                        task.timerStartTime ?: task.startTime
                    }

                    if (startTime != null) {
                        phonePickups = usageStatsReader.getPhonePickups(startTime, endTime)
                    }
                }
                _taskForReflection.value = ReflectionData(task, phonePickups)
            }
        }
    }

    fun onReflectionDialogDismiss() {
        _taskForReflection.value = null
    }

    private val _activeFocusBlock = MutableStateFlow<FocusBlock?>(null)
    val activeFocusBlock: StateFlow<FocusBlock?> = _activeFocusBlock.asStateFlow()

    private val tickerFlow = flow {
        while (true) {
            emit(Unit)
            delay(60_000L) // Check every minute
        }
    }

    val tasks: StateFlow<List<Task>> = tickerFlow
        .flatMapLatest { taskRepository.getDashboardTasks() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val completionStreak: StateFlow<Int> = taskRepository.getCompletionStreak()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val notifiedFocusBlocksStartingSoon = mutableSetOf<String>()
    private val notifiedFocusBlocksEndingSoon = mutableSetOf<String>()
    private val notifiedTasksStartingSoon = mutableSetOf<UUID>()
    private val taskPauseTimes = ConcurrentHashMap<UUID, Long>()

    // State for tracking focus block notifications
    private var hasSeenFirstFocusBlock = false
    private var lastNotifiedFocusBlockId: UUID? = null

    init {
        if (!isTest) {
            viewModelScope.launch {
                restoreRunningTasksState()
                monitorFocusBlockChanges()
                // This is the correct way to handle this.
                // When the active block changes, we need to re-evaluate the priority.
                activeFocusBlock.onEach {
                    taskRepository.updatePriorityAndWidget()
                }.launchIn(viewModelScope)

                while (isActive) {
                    _activeFocusBlock.value = taskRepository.getActiveFocusBlock().first()
                    checkTasks()
                    checkFocusBlocks()
                    delay(1000) // Check every second
                }
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

        // Helper to decide if a task is active today and should run / notify
        fun Task.isActiveForToday(): Boolean {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val startOfTomorrow = cal.timeInMillis

            val st = startTime ?: return true // inbox / unscheduled tasks: always eligible

            // Must be scheduled for *today*
            val isScheduledForToday = st in startOfToday until startOfTomorrow
            if (!isScheduledForToday) return false

            // Must have started
            if (st > now) return false

            // If not recurring or no recurrenceDays, we're done
            if (!isRecurring || recurrenceDays.isNullOrBlank()) return true

            // For recurring tasks, ensure today is in recurrenceDays
            val taskDate = Instant.ofEpochMilli(st)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val taskDayOfWeek = taskDate.dayOfWeek
            return recurrenceDays.contains(taskDayOfWeek.name, ignoreCase = true)
        }

        taskRepository.getIncompleteTasks().first().forEach { task ->
            // Auto-start only if task is active for today
            if (task.isActiveForToday()) {
                val shouldStart =
                    task.startTime != null && (task.endTime == null || now < task.endTime)
                if ((task.timerState == TimerState.NotStarted || task.timerState == TimerState.Finished) && shouldStart) {
                    startTask(task)
                }
            }

            // "Starting soon" notification: only for tasks scheduled for today
            if (task.timerState == TimerState.NotStarted && task.startTime != null) {
                val startTime = task.startTime
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfToday = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val startOfTomorrow = cal.timeInMillis
                val isToday = startTime in startOfToday until startOfTomorrow

                if (isToday) {
                    val fiveMinutesInMillis = 5 * 60 * 1000
                    if (startTime > now &&
                        startTime - now < fiveMinutesInMillis &&
                        !notifiedTasksStartingSoon.contains(task.id)
                    ) {
                        val minutesUntilStart = TimeUnit.MILLISECONDS.toMinutes(startTime - now) + 1
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
    }

    private suspend fun checkFocusBlocks() {
    val now = LocalTime.now()
    val today = LocalDate.now()
    val allFocusBlocks = taskRepository.getAllFocusBlocks()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    allFocusBlocks.forEach { focusBlock ->
        // Check if the focus block is scheduled for today
        val isTodayInRepeatDays = focusBlock.repeatDays.isEmpty() || focusBlock.repeatDays.contains(today.dayOfWeek.value)
        if (!isTodayInRepeatDays) {
            return@forEach
        }

        val context = taskRepository.getContextById(focusBlock.contextId)
        context?.let {
            val startTime = LocalTime.parse(focusBlock.startTime, formatter)
            val endTime = LocalTime.parse(focusBlock.endTime, formatter)
            val fiveMinutes = 5L

            val startingSoonKey = "start-${focusBlock.startTime}-${it.name}"
            if (now.isBefore(startTime) && now.plusMinutes(fiveMinutes)
                    .isAfter(startTime) && !notifiedFocusBlocksStartingSoon.contains(startingSoonKey)
            ) {
                val minutesUntilStart = java.time.Duration.between(now, startTime).toMinutes() + 1
                notificationService.sendFocusBlockStartingSoonNotification(
                    it.name,
                    minutesUntilStart.toInt()
                )
                hapticManager.performNudge()
                notifiedFocusBlocksStartingSoon.add(startingSoonKey)
            }

            val endingSoonKey = "end-${focusBlock.endTime}-${it.name}"
            if (now.isBefore(endTime) && now.plusMinutes(fiveMinutes)
                    .isAfter(endTime) && !notifiedFocusBlocksEndingSoon.contains(endingSoonKey)
            ) {
                val minutesUntilEnd = java.time.Duration.between(now, endTime).toMinutes() + 1
                notificationService.sendFocusBlockEndingSoonNotification(
                    it.name,
                    minutesUntilEnd.toInt()
                )
                hapticManager.performNudge()
                notifiedFocusBlocksEndingSoon.add(endingSoonKey)
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
                                timerState = TimerState.Finished,
                                isComplete = false
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
        reminderManager.cancelAllReminders(task)
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
        subTaskJob?.cancel()
    }

    fun addTask(
        title: String,
        description: String?,
        contextId: UUID?,
        priority: TaskPriority,
        isUrgent: Boolean,
        startTimeStr: String,
        endTimeStr: String,
        durationInMinutes: Int?,
        isRecurring: Boolean,
        recurrenceDays: Set<DayOfWeek>,
        isForTomorrow: Boolean = false
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
                    val (st, et, dur) = parseTimeAndCalculateDuration(startTimeStr, endTimeStr, isForTomorrow)
                    startTime = st
                    endTime = et
                    dueTime = et
                    duration = dur
                } else if (durationInMinutes != null) {
                    schedulingType = SchedulingType.Duration
                    if (startTimeStr.isNotBlank()) { // Can have start time without being recurring
                        val (st, et) = parseStartTimeAndCalculateEndTime(startTimeStr, durationInMinutes, isForTomorrow)
                        startTime = st
                        endTime = et
                        dueTime = et
                    }
                } else {
                    schedulingType = SchedulingType.None
                    if (startTimeStr.isNotBlank()) {
                        val startCal = parseDateTime(startTimeStr, isForTomorrow)
                        if (startCal != null) {
                            startTime = startCal.timeInMillis
                        }
                    } else if (isForTomorrow) {
                        // If it's a regular task for tomorrow, set the start time to the beginning of the next day.
                        val tomorrow = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        startTime = tomorrow.timeInMillis
                    }
                }

                val newId = UUID.randomUUID()
                val newTask = Task(
                    id = newId,
                    title = title,
                    description = description,
                    contextId = contextId,
                    priority = priority,
                    isUrgent = isUrgent,
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

    private fun parseDateTime(dateTimeStr: String, isForTomorrow: Boolean = false): Calendar? {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        try {
            val calendar = Calendar.getInstance()
            calendar.time = dateTimeFormat.parse(dateTimeStr) ?: return null
            return calendar
        } catch (e: Exception) {
            // Fallback to time-only parsing
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        try {
            val today = Calendar.getInstance()
            if (isForTomorrow) {
                today.add(Calendar.DAY_OF_MONTH, 1)
            }
            val calendar = Calendar.getInstance()
            calendar.time = timeFormat.parse(dateTimeStr) ?: return null
            calendar.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
            return calendar
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseStartTimeAndCalculateEndTime(startTimeStr: String, durationInMinutes: Int, isForTomorrow: Boolean = false): Pair<Long?, Long?> {
        if (startTimeStr.isBlank()) {
            return Pair(null, null)
        }

        val startCal = parseDateTime(startTimeStr, isForTomorrow) ?: return Pair(null, null)
        val startTime = startCal.timeInMillis
        val endTime = startTime + TimeUnit.MINUTES.toMillis(durationInMinutes.toLong())

        return Pair(startTime, endTime)
    }

    private fun parseTimeAndCalculateDuration(
        startTimeStr: String,
        endTimeStr: String,
        isForTomorrow: Boolean = false
    ): Triple<Long?, Long?, Int?> {
        if (startTimeStr.isBlank() || endTimeStr.isBlank()) {
            return Triple(null, null, null)
        }

        val startCal = parseDateTime(startTimeStr, isForTomorrow) ?: return Triple(null, null, null)
        val endCal = parseDateTime(endTimeStr, isForTomorrow) ?: return Triple(null, null, null)

        // If only time is provided for the end time, and it's before the start time, assume it's for the next day
        if (endCal.timeInMillis <= startCal.timeInMillis && !endTimeStr.contains(" ")) {
            endCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (endCal.timeInMillis <= startCal.timeInMillis) {
            return Triple(null, null, null)
        }

        val startTime = startCal.timeInMillis
        val endTime = endCal.timeInMillis
        val duration = ((endTime - startTime) / (1000 * 60)).toInt()

        return Triple(startTime, endTime, duration)
    }

    fun completeTask(task: Task, completionReflection: String) {
        viewModelScope.launch {
            val subTasks = taskRepository.getSubTasks(task.id).first()
            if (subTasks.any { !it.isComplete }) {
                // TODO: Show an error to the user
                return@launch
            }

            hapticManager.performSuccess()
            val reflectionToSave = if (completionReflection.isBlank()) null else completionReflection
            val updatedTask = task.copy(
                isComplete = true,
                completedAt = System.currentTimeMillis(),
                completionReflection = reflectionToSave,
                timerState = TimerState.Finished
            )
            taskRepository.updateTask(updatedTask)
            reminderManager.cancelAllReminders(updatedTask)
            taskRepository.updatePriorityAndWidget()
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
                reminderManager.cancelAllReminders(updatedTask)
            }
        }
    }

    fun loadSubTasks(parentId: UUID) {
        subTaskJob?.cancel()
        subTaskJob = viewModelScope.launch {
            taskRepository.getSubTasks(parentId).distinctUntilChanged().collect {
                _subTasks.value = it
            }
        }
    }

    fun clearSubTasks() {
        subTaskJob?.cancel()
        _subTasks.value = emptyList()
    }

    fun addSubTask(parentTask: Task, title: String) {
        viewModelScope.launch {
            val lastOrderIndex = _subTasks.value.maxOfOrNull { it.orderIndex } ?: 0
            val newSubTask = Task(
                title = title,
                contextId = parentTask.contextId,
                priority = parentTask.priority,
                isUrgent = parentTask.isUrgent,
                parentId = parentTask.id,
                orderIndex = lastOrderIndex + 1,
                description = null,
                schedulingType = SchedulingType.None,
                startTime = null,
                endTime = null,
                totalTimeInMinutes = null,
                isRecurring = false
            )
            taskRepository.insertTask(newSubTask)
        }
    }

    fun deleteSubTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }
}
