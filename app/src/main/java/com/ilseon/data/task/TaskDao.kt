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
     * ordered by priority (High, Mid, Low) and then by creation time.
     * This will power the main dashboard.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE isComplete = 0
        ORDER BY
            CASE priority
                WHEN 'High' THEN 1
                WHEN 'Mid'  THEN 2
                WHEN 'Low'  THEN 3
                ELSE 4
            END,
            createdAt ASC
    """)
    fun getIncompleteTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isComplete = 1 ORDER BY createdAt DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isComplete = 1 AND completionReflection IS NOT NULL ORDER BY completedAt DESC")
    fun getTasksWithReflections(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isComplete = 0 ORDER BY createdAt DESC")
    fun getTasks(): Flow<List<Task>>

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

    @Query("SELECT * FROM tasks WHERE timerState = 'Running'")
    suspend fun getRunningTasks(): List<Task>

    // --- Analytics Queries ---

    @Query("SELECT contextId, COUNT(*) as count FROM tasks WHERE isComplete = 1 AND completedAt BETWEEN :startTime AND :endTime GROUP BY contextId")
    suspend fun getFocusDistribution(startTime: Long, endTime: Long): List<FocusDistribution>

    @Query("SELECT AVG(endTime - startTime) FROM tasks WHERE isComplete = 1 AND schedulingType = 'TimeBlock' AND startTime IS NOT NULL AND endTime IS NOT NULL AND completedAt BETWEEN :startTime AND :endTime")
    suspend fun getAverageTimeBlockMillis(startTime: Long, endTime: Long): Double?

    @Query("SELECT AVG(completedAt - timerStartTime) FROM tasks WHERE isComplete = 1 AND schedulingType = 'Duration' AND timerStartTime IS NOT NULL AND completedAt IS NOT NULL AND completedAt BETWEEN :startTime AND :endTime")
    suspend fun getAverageDurationMillis(startTime: Long, endTime: Long): Double?

    @Query("SELECT completionReflection FROM tasks WHERE isComplete = 1 AND completionReflection IS NOT NULL AND completedAt BETWEEN :startTime AND :endTime")
    suspend fun getCompletionReflections(startTime: Long, endTime: Long): List<String>

    @Query("SELECT COUNT(*) FROM tasks WHERE isComplete = 1 AND schedulingType = 'TimeBlock' AND endTime IS NOT NULL AND completedAt > endTime AND completedAt BETWEEN :startTime AND :endTime")
    suspend fun getOverdueTasksCount(startTime: Long, endTime: Long): Int
}