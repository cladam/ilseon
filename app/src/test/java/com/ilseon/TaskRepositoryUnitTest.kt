package com.ilseon

import android.content.Context
import com.ilseon.data.task.FocusBlockDao
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.task.SettingsRepositoryImpl
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextDao
import com.ilseon.data.task.TaskDao
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.userstatus.UserStatusRepository
import com.ilseon.notifications.IReminderManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import com.ilseon.wear.WearDataSender
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class TaskRepositoryUnitTest {

    private lateinit var context: Context
    private lateinit var taskDao: TaskDao
    private lateinit var focusBlockDao: FocusBlockDao
    private lateinit var taskContextDao: TaskContextDao
    private lateinit var reminderManager: IReminderManager
    private lateinit var userStatusRepository: UserStatusRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        mockkConstructor(android.content.Intent::class)
        every { anyConstructed<android.content.Intent>().setAction(any()) } returns mockk()

        mockkObject(com.ilseon.wear.WearDataSender)
        io.mockk.coEvery { com.ilseon.wear.WearDataSender.sendPriorityTask(any(), any()) } returns Unit

        context = mockk(relaxed = true)
        taskDao = mockk(relaxed = true)
        focusBlockDao = mockk(relaxed = true)
        taskContextDao = mockk(relaxed = true)
        reminderManager = mockk(relaxed = true)
        userStatusRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        coEvery { taskDao.getIncompleteTasks() } returns flowOf(emptyList())
        coEvery { focusBlockDao.getAllFocusBlocks() } returns emptyList()
        coEvery { userStatusRepository.getStatus("user") } returns flowOf(null)

        repository = TaskRepository(
            context = context,
            taskDao = taskDao,
            focusBlockDao = focusBlockDao,
            taskContextDao = taskContextDao,
            reminderManager = reminderManager,
            userStatusRepository = userStatusRepository,
            settingsRepository = settingsRepository
        )
    }

    @org.junit.After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `createQuickFollowUpTask creates task with default message and 45 minute timer when settings empty`() = runTest {
        val defaultContextId = UUID.randomUUID()
        val personalContext = TaskContext(id = defaultContextId, name = "Personal")

        every { settingsRepository.incidentFollowUpTitle } returns flowOf("")
        every { settingsRepository.incidentFollowUpContextId } returns flowOf(null)
        every { settingsRepository.incidentFollowUpDelayMinutes } returns flowOf(45)

        coEvery { taskContextDao.getContextByName("Family") } returns null
        coEvery { taskContextDao.getContextByName("Personal") } returns personalContext

        val insertedTaskSlot = slot<Task>()
        coEvery { taskDao.insert(capture(insertedTaskSlot)) } returns Unit

        val task = repository.createQuickFollowUpTask()

        assertEquals(SettingsRepositoryImpl.DEFAULT_INCIDENT_FOLLOW_UP_TITLE, task.title)
        assertEquals(defaultContextId, task.contextId)
        assertEquals(TaskPriority.High, task.priority)
        assertTrue(task.isUrgent)
        assertEquals(SchedulingType.Duration, task.schedulingType)
        assertEquals(45, task.totalTimeInMinutes)
        assertEquals(45 * 60L, task.remainingTimeInSeconds)

        coVerify { reminderManager.scheduleDurationTaskReminders(task) }
    }

    @Test
    fun `createQuickFollowUpTask uses customized settings when configured`() = runTest {
        val configuredContextId = UUID.randomUUID()
        val configuredContext = TaskContext(id = configuredContextId, name = "Custom Relationship")
        val customTitle = "Breathe and listen with compassion."

        every { settingsRepository.incidentFollowUpTitle } returns flowOf(customTitle)
        every { settingsRepository.incidentFollowUpContextId } returns flowOf(configuredContextId.toString())
        every { settingsRepository.incidentFollowUpDelayMinutes } returns flowOf(30)

        coEvery { taskContextDao.getContext(configuredContextId) } returns configuredContext

        val insertedTaskSlot = slot<Task>()
        coEvery { taskDao.insert(capture(insertedTaskSlot)) } returns Unit

        val task = repository.createQuickFollowUpTask()

        assertEquals(customTitle, task.title)
        assertEquals(configuredContextId, task.contextId)
        assertEquals(TaskPriority.High, task.priority)
        assertTrue(task.isUrgent)
        assertEquals(30, task.totalTimeInMinutes)
        assertEquals(30 * 60L, task.remainingTimeInSeconds)

        coVerify { reminderManager.scheduleDurationTaskReminders(task) }
    }
}
