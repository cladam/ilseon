package com.ilseon.data.task

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ilseon.AppDatabase
import com.ilseon.notifications.ReminderManager
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {

    private lateinit var taskDao: TaskDao
    private lateinit var focusBlockDao: FocusBlockDao
    private lateinit var taskContextDao: TaskContextDao
    private lateinit var db: AppDatabase
    private lateinit var reminderManager: ReminderManager
    private lateinit var repository: TaskRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        taskDao = db.taskDao()
        focusBlockDao = db.focusBlockDao()
        taskContextDao = db.taskContextDao()
        reminderManager = mockk(relaxed = true)
        repository = TaskRepository(taskDao, focusBlockDao, taskContextDao, reminderManager)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertTask_forTimedTask_schedulesReminder() = runBlocking {
        val task = Task(
            title = "Timed Task",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.High,
            startTime = System.currentTimeMillis() + 10000,
            endTime = System.currentTimeMillis() + 20000
        )
        repository.insertTask(task)
        coVerify { reminderManager.scheduleTimedTaskReminders(task) }
    }

    @Test
    fun insertTask_forUntimedTask_cancelsReminder() = runBlocking {
        val task = Task(
            title = "Untimed Task",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.Medium
            // No start or end time
        )
        repository.insertTask(task)
        coVerify { reminderManager.cancelReminder(task) }
    }

    @Test
    fun updateTask_changesPersistInDatabase() = runBlocking {
        val task = Task(title = "Original Title", contextId = UUID.randomUUID(), priority = TaskPriority.Medium)
        repository.insertTask(task)

        val updatedTask = task.copy(title = "Updated Title")
        repository.updateTask(updatedTask)
        
        val retrievedTask = repository.getTaskById(task.id)
        assertEquals("Updated Title", retrievedTask?.title)
    }

    @Test
    fun deleteTask_removesTaskAndCancelsReminder() = runBlocking {
        val task = Task(title = "To Be Deleted", contextId = UUID.randomUUID(), priority = TaskPriority.Low)
        repository.insertTask(task)
        
        assertTrue(repository.getTasks().first().isNotEmpty())
        repository.deleteTask(task)

        assertNull(repository.getTaskById(task.id))
        coVerify { reminderManager.cancelReminder(task) }
    }

    @Test
    fun getIncompleteTasks_returnsOnlyUnfinishedTasks() = runBlocking {
        val incompleteTask = Task(title = "Incomplete", contextId = UUID.randomUUID(), priority = TaskPriority.High)
        val completedTask = Task(title = "Completed", contextId = UUID.randomUUID(), priority = TaskPriority.Low, isComplete = true)

        repository.insertTask(incompleteTask)
        repository.insertTask(completedTask)

        val incompleteTasks = repository.getIncompleteTasks().first()

        assertEquals(1, incompleteTasks.size)
        assertEquals("Incomplete", incompleteTasks[0].title)
    }
}
