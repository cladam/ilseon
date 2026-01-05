package com.ilseon.data.idea

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdeaRepository @Inject constructor(
    private val ideaDao: IdeaDao
) {

    fun getIdeas(): Flow<List<Idea>> {
        return ideaDao.getIdeas()
    }

    suspend fun convertIdea(id: UUID) {
        ideaDao.convertIdea(id)
    }

    suspend fun insertIdea(content: String, imageUri: String? = null, isReference: Boolean = false): UUID {
        val newIdea = Idea(
            content = content,
            imageUri = imageUri,
            isReference = isReference
        )
        ideaDao.insertIdea(newIdea)
        return newIdea.id
    }

    suspend fun updateIdea(idea: Idea) {
        ideaDao.updateIdea(idea)
    }

    suspend fun deleteIdea(id: UUID) {
        ideaDao.deleteIdea(id)
    }

    suspend fun getIdea(id: UUID): Idea? {
        return ideaDao.getIdea(id)
    }

    suspend fun getConvertedIdea(id: UUID): Idea? {
        return ideaDao.getConvertedIdea(id)
    }

    suspend fun getConvertedIdeas(): List<Idea> {
        return ideaDao.getConvertedIdeas()
    }
}
