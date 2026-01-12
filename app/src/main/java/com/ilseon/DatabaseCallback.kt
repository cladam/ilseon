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
            // Create the primary "Ilseon" context for internal guide tasks
            val ilseonContextId = UUID.randomUUID()
            val ilseonContext = TaskContext(
                id = ilseonContextId,
                name = "Ilseon",
                description = "Guide & Support",
                displayOrder = 0
            )
            taskContextDao.insertContext(ilseonContext)

            // Insert the single onboarding "First Thought" task
            taskDao.insert(
                Task(
                    title = "Capture Your First Thought",
                    contextId = ilseonContextId,
                    priority = TaskPriority.High,
                    description = "Welcome! To get started, capture a thought using the **'Idea Inbox'** or **'Voice Inbox'** option in the menu. Once you're done, check this box to complete your first task.",
                    isCurrentPriority = true
                )
            )
        }
    }
}
