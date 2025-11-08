package com.ilseon.data.task

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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
     * Gets a single task by its ID.
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: UUID): Task?
}