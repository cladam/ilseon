package com.ilseon.data.task

import com.ilseon.notifications.ReminderManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    fun getTasks(): Flow<List<Task>> = taskDao.getTasks()

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
        reminderManager.scheduleReminder(task)
    }

    suspend fun insert(task: Task) {
        taskDao.insert(task)
        reminderManager.scheduleReminder(task)
    }

    suspend fun update(task: Task) {
        taskDao.update(task)
        if (task.isComplete) {
            reminderManager.cancelReminder(task)
        } else {
            reminderManager.scheduleReminder(task)
        }
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        if (task.isComplete) {
            reminderManager.cancelReminder(task)
        } else {
            reminderManager.scheduleReminder(task)
        }
    }

    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
        reminderManager.cancelReminder(task)
    }

    suspend fun getTaskById(id: UUID): Task? {
        return taskDao.getTaskById(id)
    }

    fun getActiveFocusBlock(): Flow<FocusBlock?> {
        return focusBlockDao.getFocusBlocks().map { workBlocks ->
            val currentTime = LocalTime.now()
            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            workBlocks.find {
                val startTime = LocalTime.parse(it.startTime, formatter)
                val endTime = LocalTime.parse(it.endTime, formatter)
                !currentTime.isBefore(startTime) && currentTime.isBefore(endTime)
            }
        }
    }
}