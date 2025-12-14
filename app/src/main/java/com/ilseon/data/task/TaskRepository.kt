package com.ilseon.data.task

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ilseon.notifications.IReminderManager
import com.ilseon.widget.PriorityWidgetReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.contains
import kotlin.compareTo
import kotlin.ranges.rangeUntil
import kotlin.text.compareTo
import kotlin.text.get
import kotlin.text.set
import kotlin.times


@Singleton
class TaskRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val focusBlockDao: FocusBlockDao,
    private val taskContextDao: TaskContextDao,
    private val reminderManager: IReminderManager
) {
    fun getIncompleteTasks(): Flow<List<Task>> = taskDao.getIncompleteTasks()

    fun getSubTasks(parentId: UUID): Flow<List<Task>> = taskDao.getSubTasks(parentId)

    // Need to remove the debugging at some point
    fun getDashboardTasks(): Flow<List<Task>> {
        val tasksFlow = taskDao.getIncompleteTasks()
        val allFocusBlocksFlow = focusBlockDao.getFocusBlocks()

        return combine(tasksFlow, allFocusBlocksFlow) { tasks, allFocusBlocks ->
            val now = System.currentTimeMillis()
            val localNow = LocalTime.now()
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val todayDayOfWeek = LocalDate.now().dayOfWeek

            val activeFocusBlock = allFocusBlocks.find {
                val startTime = LocalTime.parse(it.startTime, formatter)
                val endTime = LocalTime.parse(it.endTime, formatter)
                val isTodayInRepeatDays = it.repeatDays.isEmpty() || it.repeatDays.contains(todayDayOfWeek.value)
                !localNow.isBefore(startTime) && localNow.isBefore(endTime) && isTodayInRepeatDays
            }

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val startOfTomorrow = cal.timeInMillis

            // Map recurring tasks to their "effective" start time for today
            val tasksWithEffectiveTime = tasks.map { task ->
                if (task.isRecurring && task.startTime != null && !task.recurrenceDays.isNullOrBlank()) {
                    val isTodayRecurrenceDay = task.recurrenceDays.contains(todayDayOfWeek.name, ignoreCase = true)
                    // Only adjust if today is a recurrence day AND the task's startTime is actually today
                    val taskStartDate = Instant.ofEpochMilli(task.startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val isTaskScheduledForToday = taskStartDate == LocalDate.now()

                    if (isTodayRecurrenceDay && isTaskScheduledForToday) {
                        // Calculate today's occurrence time
                        val originalCal = Calendar.getInstance().apply { timeInMillis = task.startTime }
                        val todayCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, originalCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, originalCal.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        task.copy(startTime = todayCal.timeInMillis)
                    } else {
                        task
                    }
                } else {
                    task
                }
            }

            Log.d("RepoDebug", "=== Repository Debug ===")
            tasksWithEffectiveTime.forEach { task ->
                Log.d("RepoDebug", "Task: ${task.title}, startTime: ${task.startTime}, isRecurring: ${task.isRecurring}")
            }

            val todayTasks = tasksWithEffectiveTime.filter { task ->
                if (task.startTime == null) {
                    true
                } else if (task.isRecurring && !task.recurrenceDays.isNullOrBlank()) {
                    // Only show if today is a recurrence day AND the start time is today (not a future occurrence)
                    val isRecurrenceToday = task.recurrenceDays.contains(todayDayOfWeek.name, ignoreCase = true)
                    val isStartTimeToday = task.startTime in startOfToday..<startOfTomorrow
                    isRecurrenceToday && isStartTimeToday
                } else {
                    task.startTime in startOfToday..<startOfTomorrow
                }
            }

            Log.d("RepoDebug", "Today tasks after filter: ${todayTasks.map { it.title }}")

            val eisenhowerComparator = compareByDescending<Task> { it.isUrgent }
                .thenBy { it.priority.ordinal }
                .thenBy { it.startTime ?: Long.MAX_VALUE }
                .thenBy { it.createdAt }

            if (activeFocusBlock != null) {
                val focusContextTasks = todayTasks.filter { it.contextId == activeFocusBlock.contextId }
                val urgentOutOfContext = todayTasks.filter { task ->
                    task.contextId != activeFocusBlock.contextId &&
                            task.isUrgent &&
                            !task.isComplete
                }
                (focusContextTasks + urgentOutOfContext).sortedWith(eisenhowerComparator)
            } else {
                val focusBlockContextIds = allFocusBlocks.map { it.contextId }.toSet()
                Log.d("RepoDebug", "No active focus block. Focus block context IDs: $focusBlockContextIds")

                todayTasks.filter { task ->
                    val isUrgentHigh = task.isUrgent && task.priority == TaskPriority.High
                    val inFocusBlockContext = task.contextId in focusBlockContextIds
                    val taskStart = task.startTime

                    Log.d("RepoDebug", "Filtering ${task.title}: urgentHigh=$isUrgentHigh, inFocusContext=$inFocusBlockContext, startTime=$taskStart")

                    if (isUrgentHigh) {
                        Log.d("RepoDebug", "  -> INCLUDED (urgent high)")
                        true
                    } else if (!inFocusBlockContext) {
                        Log.d("RepoDebug", "  -> INCLUDED (no focus block for context)")
                        true
                    } else {
                        if (taskStart == null) {
                            Log.d("RepoDebug", "  -> EXCLUDED (in focus context, no start time)")
                            false
                        } else {
                            val taskDate = Instant.ofEpochMilli(taskStart)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val taskDayOfWeek = taskDate.dayOfWeek.value

                            val hasBlockForTaskDay = allFocusBlocks.any { fb ->
                                fb.contextId == task.contextId &&
                                        (fb.repeatDays.isEmpty() || fb.repeatDays.contains(taskDayOfWeek))
                            }

                            Log.d("RepoDebug", "  -> ${if (!hasBlockForTaskDay) "INCLUDED" else "EXCLUDED"} (hasBlockForTaskDay=$hasBlockForTaskDay)")
                            !hasBlockForTaskDay
                        }
                    }
                }.sortedWith(eisenhowerComparator)
            }
        }
    }

    fun getIncompleteTasksByContext(contextId: UUID): Flow<List<Task>> {
        return taskDao.getIncompleteTasksByContext(contextId)
    }

    suspend fun updatePriorityAndWidget() {
        val now = System.currentTimeMillis()
        val allIncompleteTasks = taskDao.getIncompleteTasks().first()
        val validTasks = allIncompleteTasks.filter { it.startTime == null || it.startTime <= now }

        val sortedTasks = validTasks.sortedWith(
            compareBy<Task> { !it.isUrgent }
                .thenBy {
                    when (it.priority) {
                        TaskPriority.High -> 0
                        TaskPriority.Medium -> 1
                        TaskPriority.Low -> 2
                    }
                }
                .thenBy {
                    if (it.priority == TaskPriority.High) {
                        when (it.schedulingType) {
                            SchedulingType.TimeBlock -> 0
                            SchedulingType.Duration -> 1
                            SchedulingType.None -> 2
                        }
                    } else {
                        0
                    }
                }
                .thenBy { it.createdAt }
        )

        val newPriorityTask = sortedTasks.firstOrNull()
        val currentPriorityTask = allIncompleteTasks.find { it.isCurrentPriority }

        if (newPriorityTask?.id != currentPriorityTask?.id) {
            taskDao.clearCurrentPriority()
            newPriorityTask?.let {
                taskDao.setCurrentPriority(it.id)
            }
        }
        updateWidget()
    }

    fun getUnarchivedRecurringTaskSeries(): Flow<List<Task>> {
        return taskDao.getUnarchivedRecurringTaskSeries()
    }

    fun getCompletionStreak(): Flow<Int> {
        val twentyFourHoursAgo = Calendar.getInstance().apply {
            add(Calendar.HOUR, -24)
        }.timeInMillis
        return taskDao.getSuccessfulCompletionsCount(twentyFourHoursAgo)
    }

    fun getHistoricalCompletionStreaks(days: Int): Flow<Map<LocalDate, Int>> {
        val today = LocalDate.now()
        val dates = (0 until days).map { today.minusDays(it.toLong()) }
        
        return taskDao.getCompletedTasks().map { completedTasks ->
            dates.associateWith { date ->
                val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                completedTasks.count { task ->
                    task.completedAt != null && task.completedAt in startOfDay..endOfDay
                }
            }
        }
    }


    suspend fun archiveTaskSeries(task: Task) {
        task.seriesId?.let {
            taskDao.archiveTaskSeries(it)
        }
    }

    suspend fun getContextById(id: UUID): TaskContext? {
        return taskContextDao.getContext(id)
    }

    suspend fun getOrCreateImportedContext(): TaskContext {
        val existingContext = taskContextDao.getContextByName("Imported")
        if (existingContext != null) {
            return existingContext
        }
        val newContext = TaskContext(name = "Imported", description = "Tasks from imported reflections")
        taskContextDao.insertContext(newContext)
        return newContext
    }

    fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks()
    }

    fun getTasksWithReflections(): Flow<List<Task>> {
        return taskDao.getTasksWithReflections()
    }

    suspend fun getAllTasksForDebug(): List<Task> {
        return taskDao.getAllTasksForDebug()
    }

    fun getTasks(): Flow<List<Task>> = taskDao.getTasks()

    fun getCurrentPriorityTask(): Flow<Task?> = getDashboardTasks().map { tasks ->
        val currentTime = System.currentTimeMillis()
        // Only return tasks that have started (or have no start time)
        tasks.firstOrNull { task ->
            task.startTime == null || task.startTime <= currentTime
        }
    }
    suspend fun getAllFocusBlocks(): List<FocusBlock> {
        return focusBlockDao.getAllFocusBlocks()
    }

    suspend fun insertTask(task: Task) {
        taskDao.insert(task)
        updateRemindersForTask(task)
        updatePriorityAndWidget()
    }

    suspend fun insertTasks(tasks: List<Task>) {
        taskDao.insertTasks(tasks)
        updatePriorityAndWidget()
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task)
        updateRemindersForTask(task)
        updatePriorityAndWidget()

        if (task.isComplete && task.isRecurring && !task.isArchived) {
            createNewRecurringInstance(task)
        }
    }

    private suspend fun createNewRecurringInstance(task: Task) {
        if (task.startTime == null || task.recurrenceDays.isNullOrBlank()) {
            return
        }

        val recurrenceDayStrings = task.recurrenceDays
            .replace("[", "").replace("]", "")
            .split(',')
            .map { it.trim().uppercase() }

        val recurrenceDays = recurrenceDayStrings.mapNotNull { dayString ->
            try {
                when (java.time.DayOfWeek.valueOf(dayString)) {
                    java.time.DayOfWeek.SUNDAY -> Calendar.SUNDAY
                    java.time.DayOfWeek.MONDAY -> Calendar.MONDAY
                    java.time.DayOfWeek.TUESDAY -> Calendar.TUESDAY
                    java.time.DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
                    java.time.DayOfWeek.THURSDAY -> Calendar.THURSDAY
                    java.time.DayOfWeek.FRIDAY -> Calendar.FRIDAY
                    java.time.DayOfWeek.SATURDAY -> Calendar.SATURDAY
                }
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()

        if (recurrenceDays.isEmpty()) return

        val originalStartCal = Calendar.getInstance().apply { timeInMillis = task.startTime }

        // Always start searching from TOMORROW to ensure the new instance is in the future
        val nextStartCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1) // Start from tomorrow
            set(Calendar.HOUR_OF_DAY, originalStartCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, originalStartCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Find the next valid recurrence day starting from tomorrow
        for (i in 0..6) {
            if (recurrenceDays.contains(nextStartCal.get(Calendar.DAY_OF_WEEK))) {
                break
            }
            nextStartCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val duration = if (task.endTime != null && task.endTime > task.startTime) task.endTime - task.startTime else 0L
        val nextEndTime = if (task.endTime != null) nextStartCal.timeInMillis + duration else null

        val nextDueTime = when (task.schedulingType) {
            SchedulingType.TimeBlock, SchedulingType.Duration -> nextEndTime
            SchedulingType.None -> null
        }

        val newTask = task.copy(
            id = UUID.randomUUID(),
            isComplete = false,
            completedAt = null,
            completionReflection = null,
            timerState = TimerState.NotStarted,
            timerStartTime = null,
            remainingTimeInSeconds = task.totalTimeInMinutes?.times(60L) ?: 0,
            startTime = nextStartCal.timeInMillis,
            endTime = nextEndTime,
            dueTime = nextDueTime,
            seriesId = task.seriesId ?: task.id
        )
        insertTask(newTask)
    }


    suspend fun deleteTask(task: Task) {
        if (task.isRecurring) {
            task.seriesId?.let {
                taskDao.archiveTaskSeries(it)
            }
        } else {
            taskDao.delete(task)
        }
        reminderManager.cancelAllReminders(task)
        updatePriorityAndWidget()
    }

    suspend fun getTaskById(id: UUID): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun getRunningTasks(): List<Task> {
        return taskDao.getRunningTasks()
    }

    suspend fun startDurationTask(taskId: UUID) {
        val task = taskDao.getTaskById(taskId)
        if (task != null && task.totalTimeInMinutes != null && task.timerState == TimerState.NotStarted) {
            val updatedTask = task.copy(
                timerState = TimerState.Running,
                timerStartTime = System.currentTimeMillis()
            )
            taskDao.update(updatedTask)
            reminderManager.scheduleDurationTaskReminders(updatedTask)
        }
    }

    suspend fun rescheduleAllReminders() {
        val allTasks = taskDao.getTasks().first()
        for (task in allTasks) {
            updateRemindersForTask(task)
        }
    }

    private fun updateRemindersForTask(task: Task) {
        if (task.isComplete) {
            reminderManager.cancelAllReminders(task)
            return
        }

        if (task.startTime != null) {
            reminderManager.rescheduleReminders(task)
        } else {
            reminderManager.cancelAllReminders(task)
        }
    }

    fun getActiveFocusBlock(): Flow<FocusBlock?> {
        return focusBlockDao.getFocusBlocks().map { focusBlocks ->
            val now = LocalTime.now()
            val today = LocalDate.now().dayOfWeek.value
            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            focusBlocks.find {
                val startTime = LocalTime.parse(it.startTime, formatter)
                val endTime = LocalTime.parse(it.endTime, formatter)
                val isTodayInRepeatDays = it.repeatDays.isEmpty() || it.repeatDays.contains(today)

                !now.isBefore(startTime) && now.isBefore(endTime) && isTodayInRepeatDays
            }
        }
    }

    private fun updateWidget() {
        val intent = Intent(context, PriorityWidgetReceiver::class.java).apply {
            action = PriorityWidgetReceiver.UPDATE_ACTION
        }
        context.sendBroadcast(intent)
    }
}
