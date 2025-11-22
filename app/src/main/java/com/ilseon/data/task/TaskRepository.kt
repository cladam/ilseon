package com.ilseon.data.task

import com.ilseon.notifications.ReminderManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val focusBlockDao: FocusBlockDao,
    private val taskContextDao: TaskContextDao,
    private val reminderManager: ReminderManager
) {
    fun getIncompleteTasks(): Flow<List<Task>> {
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val tasksFlow = taskDao.getIncompleteTasks().map { tasks ->
            tasks.filter { task ->
                // A task is visible if it has no start time (is unscheduled)
                // or if its start time is today or in the past.
                task.startTime == null || task.startTime <= endOfToday
            }
        }

        return tasksFlow.combine(getActiveFocusBlock()) { tasks, activeFocusBlock ->
            if (activeFocusBlock != null) {
                tasks.filter { it.contextId == activeFocusBlock.contextId || it.priority == TaskPriority.High }
            } else {
                tasks
            }
        }
    }

    fun getActiveRecurringTasks(): Flow<List<Task>> {
        return taskDao.getActiveRecurringTasks()
    }

    fun getCompletionStreak(): Flow<Int> {
        val twentyFourHoursAgo = Calendar.getInstance().apply {
            add(Calendar.HOUR, -24)
        }.timeInMillis
        return taskDao.getSuccessfulCompletionsCount(twentyFourHoursAgo)
    }

    suspend fun archiveTaskSeries(task: Task) {
        task.seriesId?.let {
            taskDao.archiveTaskSeries(it)
        }
    }

    suspend fun getContextById(id: UUID): TaskContext? {
        return taskContextDao.getContext(id)
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

    suspend fun getAllFocusBlocks(): List<FocusBlock> {
        return focusBlockDao.getAllFocusBlocks()
    }

    suspend fun insertTask(task: Task) {
        taskDao.insert(task)
        updateRemindersForTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task)
        updateRemindersForTask(task)

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

        val nextStartTime = Calendar.getInstance().apply {
            timeInMillis = task.startTime
            add(Calendar.DAY_OF_YEAR, 1)
        }

        var nextDayFound = false
        for (i in 1..7) {
            if (recurrenceDays.contains(nextStartTime.get(Calendar.DAY_OF_WEEK))) {
                nextDayFound = true
                break
            }
            nextStartTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (!nextDayFound) {
            return
        }

        val duration = (task.endTime ?: task.startTime) - task.startTime
        val nextEndTime = nextStartTime.timeInMillis + duration

        val newTask = task.copy(
            id = UUID.randomUUID(),
            isComplete = false,
            completedAt = null,
            completionReflection = null,
            timerState = TimerState.NotStarted,
            timerStartTime = null,
            remainingTimeInSeconds = task.totalTimeInMinutes?.times(60L) ?: 0,
            startTime = nextStartTime.timeInMillis,
            endTime = nextEndTime,
            dueTime = nextEndTime,
            seriesId = task.seriesId
        )
        taskDao.insert(newTask)
    }

    suspend fun deleteTask(task: Task) {
        if (task.isRecurring) {
            task.seriesId?.let {
                taskDao.archiveTaskSeries(it)
            }
        } else {
            taskDao.delete(task)
        }
        reminderManager.cancelReminder(task)
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
            reminderManager.cancelReminder(task)
            return
        }

        if (task.startTime != null && task.endTime != null) {
            reminderManager.scheduleTimedTaskReminders(task)
        } else {
            reminderManager.cancelReminder(task)
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
}