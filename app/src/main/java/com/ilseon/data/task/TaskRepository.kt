package com.ilseon.data.task

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ilseon.data.EnergyLevel
import com.ilseon.data.userstatus.UserStatus
import com.ilseon.data.userstatus.UserStatusRepository
import com.ilseon.notifications.IReminderManager
import com.ilseon.wear.WearDataSender
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
    @param:ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val focusBlockDao: FocusBlockDao,
    private val taskContextDao: TaskContextDao,
    private val reminderManager: IReminderManager,
    private val userStatusRepository: UserStatusRepository
) {
    fun getIncompleteTasks(): Flow<List<Task>> = taskDao.getIncompleteTasks()

    fun getSubTasks(parentId: UUID): Flow<List<Task>> = taskDao.getSubTasks(parentId)

    fun getDashboardTasks(): Flow<List<Task>> {
        val tasksFlow = taskDao.getIncompleteTasks()
        val allFocusBlocksFlow = focusBlockDao.getFocusBlocks()
        val userStatusFlow = userStatusRepository.getStatus("user")

        return combine(tasksFlow, allFocusBlocksFlow, userStatusFlow) { tasks, allFocusBlocks, userStatus ->
            getDashboardTasks(tasks, allFocusBlocks, userStatus)
        }
    }

    private fun getDashboardTasks(
        tasks: List<Task>,
        allFocusBlocks: List<FocusBlock>,
        userStatus: UserStatus?
    ): List<Task> {
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

        val tasksWithEffectiveTime = tasks.map { task ->
            if (task.isRecurring && task.startTime != null && !task.recurrenceDays.isNullOrBlank()) {
                val isTodayRecurrenceDay = task.recurrenceDays.contains(todayDayOfWeek.name, ignoreCase = true)
                val isStoredDateTodayOrEarlier = task.startTime < startOfTomorrow

                if (isTodayRecurrenceDay && isStoredDateTodayOrEarlier) {
                    val originalCal = Calendar.getInstance().apply { timeInMillis = task.startTime }
                    val todayCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, originalCal.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, originalCal.get(Calendar.MINUTE))
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val adjustedEndTime = task.endTime?.let { originalEnd ->
                        val duration = originalEnd - task.startTime
                        todayCal.timeInMillis + duration
                    }
                    task.copy(startTime = todayCal.timeInMillis, endTime = adjustedEndTime)
                } else {
                    task
                }
            } else {
                task
            }
        }

        val todayTasks = tasksWithEffectiveTime.filter { task ->
            if (task.startTime == null) {
                true
            } else if (task.isRecurring && !task.recurrenceDays.isNullOrBlank()) {
                val isRecurrenceToday = task.recurrenceDays.contains(todayDayOfWeek.name, ignoreCase = true)
                val isStartTimeToday = task.startTime in startOfToday until startOfTomorrow
                isRecurrenceToday && isStartTimeToday
            } else {
                task.startTime in startOfToday until startOfTomorrow
            }
        }

        val energyLevel = userStatus?.currentEnergy
        val ilseonComparator = createIlseonComparator(energyLevel)

        return if (activeFocusBlock != null) {
            val focusContextTasks = todayTasks.filter { it.contextId == activeFocusBlock.contextId }
            val urgentOutOfContext = todayTasks.filter { task ->
                task.contextId != activeFocusBlock.contextId && task.isUrgent && !task.isComplete
            }
            focusContextTasks.sortedWith(ilseonComparator) + urgentOutOfContext.sortedWith(ilseonComparator)
        } else {
            val focusBlockContextIds = allFocusBlocks.map { it.contextId }.toSet()
            todayTasks.filter { task ->
                val isUrgentHigh = task.isUrgent && task.priority == TaskPriority.High
                val inFocusBlockContext = task.contextId in focusBlockContextIds
                val taskStart = task.startTime

                if (isUrgentHigh) {
                    true
                } else if (!inFocusBlockContext) {
                    true
                } else {
                    if (taskStart == null) {
                        false
                    } else {
                        val taskDate = Instant.ofEpochMilli(taskStart)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val taskDayOfWeek = taskDate.dayOfWeek.value

                        val hasBlockForTaskDay = allFocusBlocks.any { fb ->
                            fb.contextId == task.contextId && (fb.repeatDays.isEmpty() || fb.repeatDays.contains(taskDayOfWeek))
                        }
                        !hasBlockForTaskDay
                    }
                }
            }.sortedWith(ilseonComparator)
        }
    }

    private fun createIlseonComparator(energyLevel: EnergyLevel?): Comparator<Task> {
        return when (energyLevel) {
            EnergyLevel.Low -> Comparator { t1, t2 ->
                val manualCompare = compareByDescending<Task> { it.isManualPriority }.compare(t1, t2)
                if (manualCompare != 0) return@Comparator manualCompare

                compareByDescending<Task> { it.isUrgent }
                    .thenBy { it.priority.ordinal }
                    .thenBy {
                        when (it.energyLevel) {
                            EnergyLevel.Low -> 0
                            EnergyLevel.Medium -> 1
                            EnergyLevel.High -> 2
                            null -> 1
                        }
                    }
                    .thenBy { it.startTime ?: Long.MAX_VALUE }
                    .thenBy { it.createdAt }
                    .compare(t1, t2)
            }
            EnergyLevel.Medium -> Comparator { t1, t2 ->
                val manualCompare = compareByDescending<Task> { it.isManualPriority }.compare(t1, t2)
                if (manualCompare != 0) return@Comparator manualCompare

                compareByDescending<Task> { it.isUrgent }
                    .thenBy { it.priority.ordinal }
                    .thenBy {
                        when (it.energyLevel) {
                            EnergyLevel.Medium -> 0
                            EnergyLevel.Low -> 1
                            EnergyLevel.High -> 2
                            null -> 1
                        }
                    }
                    .thenBy { it.startTime ?: Long.MAX_VALUE }
                    .thenBy { it.createdAt }
                    .compare(t1, t2)
            }
            EnergyLevel.High, null -> Comparator { t1, t2 ->
                val manualCompare = compareByDescending<Task> { it.isManualPriority }.compare(t1, t2)
                if (manualCompare != 0) return@Comparator manualCompare

                compareByDescending<Task> { it.isUrgent }
                    .thenBy { it.priority.ordinal }
                    .thenBy { it.energyLevel?.ordinal ?: 1 }
                    .thenBy { it.startTime ?: Long.MAX_VALUE }
                    .thenBy { it.createdAt }
                    .compare(t1, t2)
            }
        }
    }

    suspend fun getCurrentPriorityTaskForWidget(): Task? {
        val tasks = taskDao.getIncompleteTasks().first()
        val allFocusBlocks = focusBlockDao.getAllFocusBlocks()
        val userStatus = userStatusRepository.getStatus("user").first()
        val sortedTasks = getDashboardTasks(tasks, allFocusBlocks, userStatus)
        val now = System.currentTimeMillis()
        Log.d("PriorityWidget", "getCurrentPriorityTaskForWidget: $sortedTasks")
        return sortedTasks.firstOrNull { task ->
            task.startTime == null || task.startTime <= now
        }
    }

    fun getIncompleteTasksByContext(contextId: UUID): Flow<List<Task>> {
        return taskDao.getIncompleteTasksByContext(contextId)
    }

    suspend fun updatePriorityAndWidget() {
        val newPriorityTask = getCurrentPriorityTaskForWidget()
        val allIncompleteTasks = taskDao.getIncompleteTasks().first()
        val currentPriorityTask = allIncompleteTasks.find { it.isCurrentPriority }

        if (newPriorityTask?.id != currentPriorityTask?.id) {
            taskDao.clearCurrentPriority()
            newPriorityTask?.let {
                taskDao.setCurrentPriority(it.id)
            }
        }
        updateWidget()
        updateWear(newPriorityTask)
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

    suspend fun getOrCreateExtractedContext(): TaskContext {
        val existingContext = taskContextDao.getContextByName("Extracted")
        if (existingContext != null) {
            return existingContext
        }
        val newContext = TaskContext(name = "Extracted", description = "Extracted tasks from a Voice Memo transcript")
        taskContextDao.insertContext(newContext)
        return newContext
    }

    fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks()
    }

    fun getTasksWithReflections(): Flow<List<Task>> {
        return taskDao.getTasksWithReflections()
    }

    fun getTasks(): Flow<List<Task>> = taskDao.getTasks()

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

    suspend fun clearManualPriority() {
        taskDao.clearManualPriority()
    }

    suspend fun clearExpiredManualPriorities() {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        taskDao.clearExpiredManualPriority(startOfToday)
    }



    private suspend fun createNewRecurringInstance(task: Task) {
        if (task.startTime == null || task.recurrenceDays.isNullOrBlank()) {
            return
        }

        val recurrenceDayStrings = task.recurrenceDays
            .replace("[", "")
            .replace("]", "")
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
            } catch (_: IllegalArgumentException) {
                null
            }
        }.toSet()

        if (recurrenceDays.isEmpty()) return

        val originalStartCal = Calendar.getInstance().apply { timeInMillis = task.startTime }

        val nextStartCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1) 
            set(Calendar.HOUR_OF_DAY, originalStartCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, originalStartCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

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

    private suspend fun updateWear(task: Task?) {
        WearDataSender.sendPriorityTask(context, task)
    }
}
