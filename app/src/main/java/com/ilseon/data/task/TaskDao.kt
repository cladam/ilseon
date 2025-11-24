package com.ilseon.data.task

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

// Data class for the result of the focus distribution query
data class FocusDistribution(
    val contextId: UUID,
    val count: Int
)

/**
 * This is the DAO (Data Access Object) as specified in the brief.
 * It defines all the database operations (CRUD).
 *
 * It uses Kotlin's Flow to expose a reactive stream of data,
 * which the UI (Jetpack Compose) can collect.
 */
@Dao
interface TaskDao {

    /**
     * Gets a reactive flow of all incomplete tasks,
     * ordered by priority (High, Medium, Low) and then by creation time.
     * This will power the main dashboard.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE isComplete = 0 AND isArchived = 0
        ORDER BY
            isCurrentPriority DESC,
            CASE 
                WHEN schedulingType = 'TimeBlock' AND startTime IS NOT NULL AND startTime > 0 THEN 1
                ELSE 2
            END,
            CASE priority
                WHEN 'High'   THEN 1
                WHEN 'Medium' THEN 2
                WHEN 'Low'    THEN 3
                ELSE 4
            END,
            createdAt ASC
    """)
    fun getIncompleteTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isComplete = 1 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isComplete = 1 AND completedAt BETWEEN :startTime AND :endTime AND isArchived = 0 ORDER BY completedAt DESC")
    suspend fun getCompletedTasks(startTime: Long, endTime: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE isComplete = 1 AND completionReflection IS NOT NULL AND isArchived = 0 ORDER BY completedAt DESC")
    fun getTasksWithReflections(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isComplete = 0 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCurrentPriority = 1 AND isComplete = 0 AND isArchived = 0 LIMIT 1")
    fun getCurrentPriorityTask(): Flow<Task?>

    @Query("UPDATE tasks SET isCurrentPriority = 0")
    suspend fun clearCurrentPriority()

    @Query("UPDATE tasks SET isCurrentPriority = 1 WHERE id = :taskId")
    suspend fun setCurrentPriority(taskId: UUID)

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksForDebug(): List<Task>
    
    @Query("SELECT * FROM tasks WHERE isRecurring = 1 AND isArchived = 0 AND isComplete = 0 GROUP BY seriesId")
    fun getActiveRecurringTasks(): Flow<List<Task>>

    @Query("UPDATE tasks SET isArchived = 1 WHERE seriesId = :seriesId")
    suspend fun archiveTaskSeries(seriesId: UUID)

    /**
     * Inserts a new task.
     */
    @Insert
    suspend fun insert(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    /**
     * Updates an existing task (e.g., to mark it as complete).
     */
    @Update
    suspend fun update(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    /**
     * Deletes a task.
     */
    @Delete
    suspend fun delete(task: Task)

    /**
     * Gets a single task by its ID.
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: UUID): Task?

    @Query("SELECT * FROM tasks WHERE timerState = 'Running' AND isArchived = 0")
    suspend fun getRunningTasks(): List<Task>

    // --- Analytics Queries ---

    @Query("SELECT contextId, COUNT(*) as count FROM tasks WHERE isComplete = 1 AND completedAt BETWEEN :startTime AND :endTime AND isArchived = 0 GROUP BY contextId")
    suspend fun getFocusDistribution(startTime: Long, endTime: Long): List<FocusDistribution>

    @Query("SELECT AVG(endTime - startTime) FROM tasks WHERE isComplete = 1 AND schedulingType = 'TimeBlock' AND startTime IS NOT NULL AND endTime IS NOT NULL AND completedAt BETWEEN :startTime AND :endTime AND isArchived = 0")
    suspend fun getAverageTimeBlockMillis(startTime: Long, endTime: Long): Double?

    @Query("SELECT AVG(totalTimeInMinutes * 60000.0) FROM tasks WHERE isComplete = 1 AND schedulingType = 'Duration' AND totalTimeInMinutes IS NOT NULL AND completedAt BETWEEN :startTime AND :endTime AND isArchived = 0")
    suspend fun getAverageDurationMillis(startTime: Long, endTime: Long): Double?

    @Query("SELECT completionReflection FROM tasks WHERE isComplete = 1 AND completionReflection IS NOT NULL AND completedAt BETWEEN :startTime AND :endTime AND isArchived = 0")
    suspend fun getCompletionReflections(startTime: Long, endTime: Long): List<String>

    @Query("SELECT COUNT(*) FROM tasks WHERE isComplete = 1 AND schedulingType = 'TimeBlock' AND endTime IS NOT NULL AND completedAt > endTime AND completedAt BETWEEN :startTime AND :endTime AND isArchived = 0")
    suspend fun getOverdueTasksCount(startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isComplete = 1 AND completionReflection IS NOT NULL AND completedAt >= :startTime AND isArchived = 0")
    fun getSuccessfulCompletionsCount(startTime: Long): Flow<Int>

    @Query("SELECT completedAt FROM tasks WHERE isComplete = 1 AND completedAt IS NOT NULL AND isArchived = 0 ORDER BY completedAt DESC")
    fun getCompletionDates(): Flow<List<Long>>
}
