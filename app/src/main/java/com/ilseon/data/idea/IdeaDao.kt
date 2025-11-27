package com.ilseon.data.idea

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

    @Query("UPDATE idea SET content = :content WHERE id = :id")
    suspend fun updateIdea(id: UUID, content: String)

    @Insert
    suspend fun insertIdea(idea: Idea)

    @Query("SELECT * FROM idea WHERE id = :id")
    suspend fun getIdea(id: UUID): Idea?

    @Query("SELECT * from idea WHERE id = :id AND isConverted = 1")
    suspend fun getConvertedIdea(id: UUID): Idea?

    @Query("SELECT * FROM idea WHERE isConverted = 1")
    suspend fun getConvertedIdeas(): List<Idea>

}