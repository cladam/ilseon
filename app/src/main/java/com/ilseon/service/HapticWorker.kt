package com.ilseon.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class HapticWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val notificationService: NotificationService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val incompleteTasks = taskRepository.getIncompleteTasks().first()

        // Find all overdue, high-priority tasks that haven't been completed
        val overdueHighPriorityTasks = incompleteTasks.filter {
            !it.isComplete &&
            it.priority == TaskPriority.High &&
            it.dueTime != null &&
            it.dueTime < now
        }

        overdueHighPriorityTasks.forEach { task ->
            notificationService.sendNaggingNotification(task)
        }

        return Result.success()
    }
}