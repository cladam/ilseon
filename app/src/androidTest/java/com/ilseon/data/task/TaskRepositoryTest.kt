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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
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
    fun updateTask_whenRecurringTaskCompleted_createsNewInstanceOfNextDay() = runBlocking {
        // Arrange
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        val originalStartTime = calendar.timeInMillis

        val task = Task(
            title = "Weekly Report",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.Medium,
            isComplete = false,
            isRecurring = true,
            recurrenceDays = "TUESDAY,THURSDAY",
            startTime = originalStartTime
        )
        repository.insertTask(task)

        // Act: Complete the task
        val completedTask = task.copy(isComplete = true)
        repository.updateTask(completedTask)

        // Assert
        val allTasks = repository.getTasks().first()
        val newInstance = allTasks.find { !it.isComplete }

        assertNotNull("A new task instance should have been created", newInstance)
        assertEquals("TUESDAY,THURSDAY", newInstance!!.recurrenceDays)
        assertFalse(newInstance.isComplete)

        val newCalendar = Calendar.getInstance().apply { timeInMillis = newInstance.startTime!! }
        assertEquals(Calendar.THURSDAY, newCalendar.get(Calendar.DAY_OF_WEEK))

        val daysDifference = (newInstance.startTime!! - originalStartTime) / (1000 * 60 * 60 * 24)
        assertEquals(2, daysDifference)
    }

    @Test
    fun updateTask_whenRecurringTaskCompletedOnLastDay_createsNewInstanceOfNextWeek() = runBlocking {
        // Arrange
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
        val originalStartTime = calendar.timeInMillis

        val task = Task(
            title = "Weekly Report",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.Medium,
            isComplete = false,
            isRecurring = true,
            recurrenceDays = "TUESDAY,THURSDAY",
            startTime = originalStartTime
        )
        repository.insertTask(task)

        // Act: Complete the task
        val completedTask = task.copy(isComplete = true)
        repository.updateTask(completedTask)

        // Assert
        val allTasks = repository.getTasks().first()
        val newInstance = allTasks.find { !it.isComplete }

        assertNotNull("A new task instance should have been created", newInstance)
        val newCalendar = Calendar.getInstance().apply { timeInMillis = newInstance!!.startTime!! }
        assertEquals(Calendar.TUESDAY, newCalendar.get(Calendar.DAY_OF_WEEK))
        
        val daysDifference = (newInstance?.startTime!! - originalStartTime) / (1000 * 60 * 60 * 24)
        assertEquals(5, daysDifference)
    }

    @Test
    fun updateTask_whenRecurringTaskCompletedOnSameDay_createsInstanceForNextWeek() = runBlocking {
        // Arrange
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        val originalStartTime = calendar.timeInMillis

        val task = Task(
            title = "Weekly Standup",
            contextId = UUID.randomUUID(),
            priority = TaskPriority.High,
            isComplete = false,
            isRecurring = true,
            recurrenceDays = "TUESDAY", // Only recurs on Tuesday
            startTime = originalStartTime
        )
        repository.insertTask(task)

        // Act: Complete the task
        val completedTask = task.copy(isComplete = true)
        repository.updateTask(completedTask)

        // Assert
        val allTasks = repository.getTasks().first()
        val newInstance = allTasks.find { !it.isComplete }

        assertNotNull("A new task instance should have been created", newInstance)
        val newCalendar = Calendar.getInstance().apply { timeInMillis = newInstance!!.startTime!! }
        
        // The new task should be on the Tuesday of the following week.
        assertEquals(Calendar.TUESDAY, newCalendar.get(Calendar.DAY_OF_WEEK))
        
        // There should be exactly 7 days between the original and new start time.
        val daysDifference = (newInstance?.startTime!! - originalStartTime) / (1000 * 60 * 60 * 24)
        assertEquals(7, daysDifference)
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

    @Test
    fun getCompletedTasks_returnsOnlyFinishedTasks() = runBlocking {
        val incompleteTask = Task(title = "Incomplete", contextId = UUID.randomUUID(), priority = TaskPriority.High)
        val completedTask = Task(title = "Completed", contextId = UUID.randomUUID(), priority = TaskPriority.Low, isComplete = true)

        repository.insertTask(incompleteTask)
        repository.insertTask(completedTask)

        val completedTasks = repository.getCompletedTasks().first()

        assertEquals(1, completedTasks.size)
        assertEquals("Completed", completedTasks[0].title)
    }

    @Test
    fun getTasksWithReflections_returnsOnlyTasksWithReflections() = runBlocking {
        val contextId = UUID.randomUUID()
        val taskWithReflection = Task(title = "With Reflection", contextId = contextId, priority = TaskPriority.High, isComplete = true, completionReflection = "Done.")
        val taskWithoutReflection = Task(title = "No Reflection", contextId = contextId, priority = TaskPriority.Low, isComplete = true, completionReflection = null)
        val incompleteTask = Task(title = "Incomplete", contextId = contextId, priority = TaskPriority.Medium)

        repository.insertTask(taskWithReflection)
        repository.insertTask(taskWithoutReflection)
        repository.insertTask(incompleteTask)

        val tasksWithReflections = repository.getTasksWithReflections().first()

        assertEquals(1, tasksWithReflections.size)
        assertEquals("With Reflection", tasksWithReflections[0].title)
        assertNotNull(tasksWithReflections[0].completionReflection)
    }

    @Test
    fun getIncompleteTasks_withActiveFocusBlock_returnsFilteredTasksAndHighPriorityTasks() = runBlocking {
        // 1. Setup Contexts and Focus Block
        val workContext = TaskContext(name = "Work")
        val personalContext = TaskContext(name = "Personal")
        taskContextDao.insertContext(workContext)
        taskContextDao.insertContext(personalContext)

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val now = LocalTime.now()
        val activeBlock = FocusBlock(
            contextId = workContext.id,
            startTime = now.minusHours(1).format(formatter),
            endTime = now.plusHours(1).format(formatter),
            repeatDays = emptyList()
        )
        focusBlockDao.insert(activeBlock)

        // 2. Setup Tasks
        val workTask = Task(title = "Work Task", contextId = workContext.id, priority = TaskPriority.Medium)
        val personalTask = Task(title = "Personal Task", contextId = personalContext.id, priority = TaskPriority.Medium)
        val highPriorityPersonalTask = Task(title = "High Priority Personal", contextId = personalContext.id, priority = TaskPriority.High)
        val completedWorkTask = Task(title = "Completed Work", contextId = workContext.id, priority = TaskPriority.Medium, isComplete = true)

        repository.insertTask(workTask)
        repository.insertTask(personalTask)
        repository.insertTask(highPriorityPersonalTask)
        repository.insertTask(completedWorkTask)

        // 3. Act and Assert
        val filteredTasks = repository.getIncompleteTasks().first()
        for (task in filteredTasks) {
            println("Task: ${task.title}, Priority: ${task.priority}")
            println(task)
        }
        
        assertEquals(2, filteredTasks.size)
        assertTrue(filteredTasks.any { it.title == "Work Task" })
        assertTrue(filteredTasks.any { it.title == "High Priority Personal" })
    }
}