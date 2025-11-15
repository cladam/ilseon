package com.ilseon.data.task

import com.ilseon.notifications.ReminderManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val focusBlockDao: FocusBlockDao,
    private val reminderManager: ReminderManager
) {
    fun getIncompleteTasks(): Flow<List<Task>> {
        return taskDao.getIncompleteTasks().combine(getActiveFocusBlock()) { tasks, activeFocusBlock ->
            if (activeFocusBlock != null) {
                tasks.filter { it.contextId == activeFocusBlock.contextId || it.priority == TaskPriority.High }
            } else {
                tasks
            }
        }
    }

    fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks()
    }

    fun getTasksWithReflections(): Flow<List<Task>> {
        return taskDao.getTasksWithReflections()
    }

    fun getTasks(): Flow<List<Task>> = taskDao.getTasks()

    suspend fun insertTask(task: Task) {
        taskDao.insert(task)
        updateRemindersForTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task)
        updateRemindersForTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
        reminderManager.cancelReminder(task)
    }

    suspend fun getTaskById(id: UUID): Task? {
        return taskDao.getTaskById(id)
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

        // Rule 2: Task with a Scheduled Start & End Time
        if (task.startTime != null && task.endTime != null) {
            reminderManager.scheduleTimedTaskReminders(task)
        }
        // Rule 3: A duration task that is running has its reminders set when started.
        // We don't need to reschedule them on every update unless properties change.
        // For now, we assume startDurationTask is the only entry point for these reminders.

        // Rule 1 & others: For any other case, cancel reminders to be safe.
        // This includes simple notes and duration tasks that haven't been started.
        else {
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