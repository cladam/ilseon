package com.ilseon

import android.content.Context
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.FocusBlockDao
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskContext
import com.ilseon.data.task.TaskContextDao
import com.ilseon.data.task.TaskDao
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.userstatus.UserStatusRepository
import com.ilseon.notifications.IReminderManager
import com.ilseon.wear.WearDataSender
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ActiveContextIncidentFollowUpTest {

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

        mockkObject(WearDataSender)
        coEvery { WearDataSender.sendPriorityTask(any(), any()) } returns Unit

        context = mockk(relaxed = true)
        taskDao = mockk(relaxed = true)
        focusBlockDao = mockk(relaxed = true)
        taskContextDao = mockk(relaxed = true)
        reminderManager = mockk(relaxed = true)
        userStatusRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        coEvery { taskDao.getIncompleteTasks() } returns flowOf(emptyList())
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

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `createQuickFollowUpTask automatically uses the active focus block context so it is visible immediately`() = runTest {
        val activeContextId = UUID.randomUUID()
        val activeContext = TaskContext(id = activeContextId, name = "Deep Work")

        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val startTime = now.minusMinutes(10).format(formatter)
        val endTime = now.plusMinutes(50).format(formatter)

        val activeBlock = FocusBlock(
            id = UUID.randomUUID(),
            contextId = activeContextId,
            startTime = startTime,
            endTime = endTime,
            repeatDays = emptyList()
        )

        coEvery { focusBlockDao.getFocusBlocks() } returns flowOf(listOf(activeBlock))
        coEvery { focusBlockDao.getAllFocusBlocks() } returns listOf(activeBlock)
        coEvery { taskContextDao.getContext(activeContextId) } returns activeContext

        every { settingsRepository.incidentFollowUpTitle } returns flowOf("Incident grounding message")
        every { settingsRepository.incidentFollowUpContextId } returns flowOf(UUID.randomUUID().toString()) // even if custom setting exists
        every { settingsRepository.incidentFollowUpDelayMinutes } returns flowOf(45)

        val insertedTaskSlot = slot<Task>()
        coEvery { taskDao.insert(capture(insertedTaskSlot)) } returns Unit

        val task = repository.createQuickFollowUpTask()

        // Verifies that the task was assigned to the active focus context
        assertEquals(activeContextId, task.contextId)
        assertTrue(task.isUrgent)
        assertEquals(TaskPriority.High, task.priority)
    }
}
