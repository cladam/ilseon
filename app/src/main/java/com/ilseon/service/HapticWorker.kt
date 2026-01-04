package com.ilseon.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.di.WorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

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
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        val todayDayOfWeek = LocalDate.now().dayOfWeek

        val incompleteTasks = taskRepository.getIncompleteTasks().first()
        val activeFocusBlock = taskRepository.getActiveFocusBlock().first()

        val fourHoursAgo = now - TimeUnit.HOURS.toMillis(4)
        val twentyFourHoursAgo = now - TimeUnit.HOURS.toMillis(24)

        val tasksToNudge = incompleteTasks.filter { task ->
            if (task.isComplete || task.priority != TaskPriority.High) {
                return@filter false
            }

            if (task.startTime != null && task.startTime > now) {
                return@filter false
            }

            // Skip recurring tasks not scheduled for today
            if (task.isRecurring && !task.recurrenceDays.isNullOrBlank()) {
                val recurrenceDayStrings = task.recurrenceDays
                    .replace("[", "").replace("]", "")
                    .split(',')
                    .map { it.trim().uppercase() }

                val recurrenceDays = recurrenceDayStrings.mapNotNull { dayString ->
                    try {
                        DayOfWeek.valueOf(dayString)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }.toSet()

                if (recurrenceDays.isNotEmpty() && !recurrenceDays.contains(todayDayOfWeek)) {
                    return@filter false
                }
            }

            val isOverdue = task.dueTime != null &&
                    task.dueTime >= startOfToday &&
                    task.dueTime < now

            val isUnscheduledUrgent = task.dueTime == null &&
                    task.isUrgent &&
                    task.createdAt < fourHoursAgo

            val isUnscheduledNotUrgent = task.dueTime == null &&
                    !task.isUrgent &&
                    task.createdAt < twentyFourHoursAgo

            isOverdue || isUnscheduledUrgent || isUnscheduledNotUrgent
        }

        val tasksToNotify = if (activeFocusBlock != null) {
            tasksToNudge.filter { it.contextId == activeFocusBlock.contextId }
        } else {
            tasksToNudge
        }

        tasksToNotify.forEach { task ->
            notificationService.sendNaggingNotification(task)
        }

        return Result.success()
    }

}
