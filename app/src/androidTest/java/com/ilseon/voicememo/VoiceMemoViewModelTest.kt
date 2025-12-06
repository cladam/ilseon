package com.ilseon.voicememo

import android.Manifest
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.ilseon.AppDatabase
import com.ilseon.VoiceMemoViewModel
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.voicememo.VoiceMemo
import com.ilseon.data.voicememo.VoiceMemoDao
import com.ilseon.data.voicememo.VoiceMemoRepository
import com.ilseon.notifications.IReminderManager
import com.ilseon.service.ISpeechTranscriber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// Fake transcriber that returns a predictable result for testing
class FakeSpeechTranscriber(private val transcription: String) : ISpeechTranscriber {
    override suspend fun transcribe(filePath: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        onResult(transcription)
    }
}

@RunWith(AndroidJUnit4::class)
class VoiceMemoViewModelTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private lateinit var db: AppDatabase
    private lateinit var voiceMemoDao: VoiceMemoDao
    private lateinit var taskRepository: TaskRepository
    private lateinit var voiceMemoRepository: VoiceMemoRepository
    private lateinit var viewModel: VoiceMemoViewModel

    private lateinit var testFile: File
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        voiceMemoDao = db.voiceMemoDao()
        taskRepository = TaskRepository(context, db.taskDao(), db.focusBlockDao(), db.taskContextDao(), FakeReminderManager())
        voiceMemoRepository = VoiceMemoRepository(voiceMemoDao)

        // For this test, we don't need a real audio file, just a placeholder path
        testFile = File(context.cacheDir, "test_audio.m4a")
        testFile.createNewFile()
    }

    @After
    fun tearDown() {
        db.close()
        if (testFile.exists()) {
            testFile.delete()
        }
    }

    @Test
    fun testTranscriptionAndStorage() = runBlocking {
        // Arrange
        val expectedTranscription = "This is a test transcription."
        val fakeTranscriber = FakeSpeechTranscriber(expectedTranscription)
        viewModel = VoiceMemoViewModel(voiceMemoRepository, taskRepository, fakeTranscriber)

        // Act
        viewModel.startTranscription(testFile.absolutePath, 5)

        // Assert
        val memos = viewModel.voiceMemos.first { it.isNotEmpty() }
        assertEquals(1, memos.size)
        assertEquals(expectedTranscription, memos[0].transcription)
        assertEquals(testFile.absolutePath, memos[0].filePath)
    }

    @Test
    fun deleteVoiceMemo_deletesFileAndDatabaseEntry() = runBlocking {
        // Arrange - a viewmodel with a dummy transcriber and an existing memo
        viewModel =
            VoiceMemoViewModel(voiceMemoRepository, taskRepository, FakeSpeechTranscriber(""))
        val existingMemo = VoiceMemo(transcription = "memo to delete", filePath = testFile.absolutePath, durationSeconds = 1)
        voiceMemoDao.insert(existingMemo)
        // Wait for the memo to appear in the flow
        val memosBefore = viewModel.voiceMemos.first { it.isNotEmpty() }
        assertEquals(1, memosBefore.size)
        assertTrue("Test file must exist before deletion", testFile.exists())

        // Act
        viewModel.deleteVoiceMemo(memosBefore[0])

        // Assert
        // Wait for the memo list to become empty
        viewModel.voiceMemos.first { it.isEmpty() }
        assertFalse("Audio file should be deleted from storage", testFile.exists())
    }

    @Test
    fun convertToTask_deletesFileAndDatabaseEntryAndCreatesTask() = runBlocking {
        // Arrange
        viewModel =
            VoiceMemoViewModel(voiceMemoRepository, taskRepository, FakeSpeechTranscriber(""))
        val existingMemo = VoiceMemo(transcription = "This will become a task", filePath = testFile.absolutePath, durationSeconds = 1)
        voiceMemoDao.insert(existingMemo)
        // Wait for the memo to appear in the flow
        val memosBefore = viewModel.voiceMemos.first { it.isNotEmpty() }
        assertEquals(1, memosBefore.size)
        assertTrue("Test file must exist before conversion", testFile.exists())

        // Act
        viewModel.convertToTask(memosBefore[0])

        // Assert
        // 1. Voice memo and file are deleted
        viewModel.voiceMemos.first { it.isEmpty() }
        assertFalse("Audio file should be deleted after conversion", testFile.exists())

        // 2. A new task was created
        val tasks = taskRepository.getIncompleteTasks().first()
        assertEquals(1, tasks.size)
        assertEquals(existingMemo.transcription, tasks[0].title)
    }
}

class FakeReminderManager : IReminderManager {
    override fun rescheduleReminders(task: com.ilseon.data.task.Task) {}
    override fun scheduleTimedTaskReminders(task: com.ilseon.data.task.Task) {}
    override fun scheduleDurationTaskReminders(task: com.ilseon.data.task.Task) {}
    override fun cancelReminder(task: com.ilseon.data.task.Task) {}
}
