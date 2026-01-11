package com.ilseon.data.idea

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ilseon.data.ContextDistribution
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface IdeaDao {

    @Query("SELECT * FROM idea WHERE isConverted = 0 ORDER BY createdAt DESC")
    fun getIdeas(): Flow<List<Idea>>

    @Query("UPDATE idea SET isConverted = 1 WHERE id = :id")
    suspend fun convertIdea(id: UUID)

    @Query("DELETE FROM idea WHERE id = :id")
    suspend fun deleteIdea(id: UUID)

    @Update
    suspend fun updateIdea(idea: Idea)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: Idea)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdeas(ideas: List<Idea>)

    @Query("SELECT * FROM idea WHERE id = :id")
    suspend fun getIdea(id: UUID): Idea?

    @Query("SELECT * from idea WHERE id = :id AND isConverted = 1")
    suspend fun getConvertedIdea(id: UUID): Idea?

    @Query("SELECT * FROM idea WHERE isConverted = 1")
    suspend fun getConvertedIdeas(): List<Idea>

    @Query("SELECT COUNT(*) FROM idea WHERE createdAt BETWEEN :startTime AND :endTime")
    suspend fun getIdeasCount(startTime: Long, endTime: Long): Int

    @Query("SELECT contextId, COUNT(*) as count FROM idea WHERE createdAt BETWEEN :startTime AND :endTime AND contextId IS NOT NULL GROUP BY contextId")
    suspend fun getContextDistribution(startTime: Long, endTime: Long): List<ContextDistribution>

}
