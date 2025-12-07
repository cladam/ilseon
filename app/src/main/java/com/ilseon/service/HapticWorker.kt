package com.ilseon.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.di.WorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.Calendar

// TODO: Revert to @HiltWorker and @AssistedInject once androidx.hilt:hilt-compiler supports Kotlin 2.2+
class HapticWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val taskRepository: TaskRepository
    private val notificationService: NotificationService

    init {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java
        )
        taskRepository = entryPoint.taskRepository()
        notificationService = entryPoint.notificationService()
    }

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        val incompleteTasks = taskRepository.getIncompleteTasks().first()
        val activeFocusBlock = taskRepository.getActiveFocusBlock().first()

        val overdueHighPriorityTasks = incompleteTasks.filter {
            !it.isComplete &&
                    it.priority == TaskPriority.High &&
                    it.dueTime != null &&
                    it.dueTime >= startOfToday && // Due today
                    it.dueTime < now // And is overdue
        }

        val tasksToNotify = if (activeFocusBlock != null) {
            overdueHighPriorityTasks.filter { it.contextId == activeFocusBlock.contextId }
        } else {
            overdueHighPriorityTasks
        }

        tasksToNotify.forEach { task ->
            notificationService.sendNaggingNotification(task)
        }

        return Result.success()
    }
}
