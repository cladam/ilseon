package com.ilseon

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.SchedulingType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider

class DatabaseCallback @Inject constructor(
    private val database: Provider<AppDatabase>
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val taskDao = database.get().taskDao()
        val taskContextDao = database.get().taskContextDao()

        CoroutineScope(Dispatchers.IO).launch {
            // Pre-populate with ilseon contexts and tasks
            val ilseonContextId = UUID.randomUUID()
            val ilseonContext = TaskContext(id = ilseonContextId, name = "Ilseon", description = "Ilseon Context", displayOrder = 4)
            taskContextDao.insertContext(ilseonContext)
            taskDao.insert(
                Task(
                    title = "Welcome to Ilseon!",
                    contextId = ilseonContextId,
                    priority = TaskPriority.High,
                    description = "This is a regular task, click the checkbox to complete it. If you add a reflection of how the task went it will show up in Notes.",
                    isCurrentPriority = true
                )
            )
            taskDao.insert(
                Task(
                    title = "Create a new context",
                    contextId = ilseonContextId,
                    priority = TaskPriority.High,
                    description = "Contexts are powerful tools to group your tasks. You can create contexts for work, home, or any project you're working on. Just go to the Contexts screen and add a context, with or without a <b>Focus Block</b>."
                )
            )
            taskDao.insert(
                Task(
                    title = "Schedule a task with a time block",
                    contextId = ilseonContextId,
                    priority = TaskPriority.Medium,
                    description = "This task is scheduled for a specific time block.",
                    schedulingType = SchedulingType.TimeBlock,
                    startTime = System.currentTimeMillis() + 3600000, // 1 hour from now
                    endTime = System.currentTimeMillis() + 7200000 // 2 hours from now
                )
            )
            taskDao.insert(
                Task(
                    title = "Schedule a task with a duration",
                    contextId = ilseonContextId,
                    priority = TaskPriority.Medium,
                    description = "This task has a duration of 1 hour.",
                    schedulingType = SchedulingType.Duration,
                    totalTimeInMinutes = 25
                )
            )
            taskDao.insert(
                Task(
                    title = "Check your patterns",
                    contextId = ilseonContextId,
                    priority = TaskPriority.Low,
                    description = "Make sure to check the Analytics screen to see your patterns and how you're spending your time."
                )
            )
        }
    }
}
