package com.ilseon.data.task

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TaskContextDao {
    @Query("SELECT * FROM task_contexts ORDER BY displayOrder ASC")
    fun getContexts(): Flow<List<TaskContext>>

    @Query("SELECT * FROM task_contexts WHERE id = :id")
    suspend fun getContext(id: UUID): TaskContext?
    
    @Query("SELECT * FROM task_contexts WHERE name = :name LIMIT 1")
    suspend fun getContextByName(name: String): TaskContext?

    @Insert
    suspend fun insertContext(context: TaskContext)

    @Update
    suspend fun updateContext(context: TaskContext)

    @Query("DELETE FROM task_contexts WHERE id = :id")
    suspend fun deleteContext(id: UUID)
}
