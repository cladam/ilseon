package com.ilseon.data.task

import android.content.Context
import android.content.Intent
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


@Singleton
class TaskRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val focusBlockDao: FocusBlockDao,
    private val taskContextDao: TaskContextDao,
    private val reminderManager: IReminderManager
) {
    fun getIncompleteTasks(): Flow<List<Task>> = taskDao.getIncompleteTasks()

    fun getDashboardTasks(): Flow<List<Task>> {
        val tasksFlow = taskDao.getIncompleteTasks()
        val allFocusBlocksFlow = focusBlockDao.getFocusBlocks()

        return combine(tasksFlow, allFocusBlocksFlow) { tasks, allFocusBlocks ->
            // Log all incomplete tasks before filtering
            android.util.Log.d("TaskRepository", "All Incomplete Tasks: ${tasks.joinToString("\n")}")

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis

            cal.add(Calendar.DAY_OF_YEAR, 1)
            val startOfTomorrow = cal.timeInMillis

            val now = System.currentTimeMillis()
            val localNow = LocalTime.now()
            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            val activeFocusBlock = allFocusBlocks.find {
                val startTime = LocalTime.parse(it.startTime, formatter)
                val endTime = LocalTime.parse(it.endTime, formatter)
                val todayDayOfWeek = LocalDate.now().dayOfWeek.value
                val isTodayInRepeatDays = it.repeatDays.isEmpty() || it.repeatDays.contains(todayDayOfWeek)
                !localNow.isBefore(startTime) && localNow.isBefore(endTime) && isTodayInRepeatDays
            }

            val todayTasks = tasks.filter { task ->
                if (task.startTime == null) {
                    true // Always include tasks without a start time (e.g. Inbox tasks)
                } else {
                    val isScheduledForToday = task.startTime in startOfToday..<startOfTomorrow
                    if (!isScheduledForToday) {
                        false // Exclude if not scheduled for today
                    } else {
                        // It is scheduled for today, now check if its start time has passed
                        val hasStarted = task.startTime <= now
                        if (!hasStarted) {
                            false // Exclude if start time is in the future
                        } else {
                            // It's for today and has started. Now check recurrence.
                            if (!task.isRecurring || task.recurrenceDays.isNullOrBlank()) {
                                true // Not a recurring task, so include it
                            } else {
                                // It's a recurring task, check if today is a recurrence day
                                val taskDate = Instant.ofEpochMilli(task.startTime)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                val taskDayOfWeek = taskDate.dayOfWeek
                                task.recurrenceDays.contains(taskDayOfWeek.name, ignoreCase = true)
                            }
                        }
                    }
                }
            }

            if (activeFocusBlock != null) {
                val focusContextTasks = todayTasks.filter { it.contextId == activeFocusBlock.contextId }
                val urgentHighPriorityTasks = todayTasks.filter {
                    it.isUrgent && it.priority == TaskPriority.High && it.contextId != activeFocusBlock.contextId
                }
                val sortedFocusTasks = focusContextTasks.sortedWith(
                    compareBy { !it.isCurrentPriority }
                )
                sortedFocusTasks + urgentHighPriorityTasks
            } else {
                val focusBlockContextIds = allFocusBlocks.map { it.contextId }.toSet()
                todayTasks.filter { task ->
                    val isUrgentHigh = task.isUrgent && task.priority == TaskPriority.High
                    if (isUrgentHigh) {
                        // Always show urgent+high tasks
                        true
                    } else if (task.contextId !in focusBlockContextIds) {
                        // No focus block at all for this context
                        true
                    } else {
                        // Context has focus blocks; check if any apply to the task's day
                        val taskStart = task.startTime
                        if (taskStart == null) {
                            false
                        } else {
                            val taskDate = Instant.ofEpochMilli(taskStart)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            val taskDayOfWeek = taskDate.dayOfWeek.value

                            // If there is any block for this context whose repeatDays is empty (every day)
                            // or contains the task's day -> hide the task outside of active block mode
                            val hasBlockForTaskDay = allFocusBlocks.any { fb ->
                                fb.contextId == task.contextId &&
                                        (fb.repeatDays.isEmpty() || fb.repeatDays.contains(taskDayOfWeek))
                            }

                            !hasBlockForTaskDay
                        }
                    }
                }

//                todayTasks.filter { task ->
//                    if (task.contextId !in focusBlockContextIds) {
//                        true
//                    } else {
//                        // If task is in a focus context, check if there's a block for its day
//                        val taskDate = Instant.ofEpochMilli(task.startTime ?: 0).atZone(ZoneId.systemDefault()).toLocalDate()
//                        val taskDayOfWeek = taskDate.dayOfWeek.value
//                        !allFocusBlocks.any { fb ->
//                            fb.contextId == task.contextId && (fb.repeatDays.isEmpty() || fb.repeatDays.contains(taskDayOfWeek))
//                        }
//                    }
//                }
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
                }.thenBy { it.createdAt }
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

    fun getCurrentPriorityTask(): Flow<Task?> = getDashboardTasks().map { it.firstOrNull() }

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

        // The search for the next recurrence should start from either today or the task's original
        // start date, whichever is later. This handles both overdue and pre-completed tasks.
        val nextStartCal = Calendar.getInstance() // Start with now
        // Preserve the original time of day
        nextStartCal.set(Calendar.HOUR_OF_DAY, originalStartCal.get(Calendar.HOUR_OF_DAY))
        nextStartCal.set(Calendar.MINUTE, originalStartCal.get(Calendar.MINUTE))
        nextStartCal.set(Calendar.SECOND, originalStartCal.get(Calendar.SECOND))
        nextStartCal.set(Calendar.MILLISECOND, originalStartCal.get(Calendar.MILLISECOND))

        // If the task was completed in advance, start searching from its original start date.
        if (originalStartCal.after(nextStartCal)) {
            nextStartCal.timeInMillis = originalStartCal.timeInMillis
        }

        // Now, find the next valid day, starting from the day *after* our calculated start date.
        for (i in 1..7) {
            nextStartCal.add(Calendar.DAY_OF_YEAR, 1)
            if (recurrenceDays.contains(nextStartCal.get(Calendar.DAY_OF_WEEK))) {
                break // Found the next day
            }
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
            seriesId = task.seriesId ?: task.id // Ensure seriesId is set for the new instance
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
