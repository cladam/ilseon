package com.ilseon.data.task

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskContextRepository @Inject constructor(private val taskContextDao: TaskContextDao) {

    fun getContexts(): Flow<List<TaskContext>> = taskContextDao.getContexts()

    suspend fun addContext(name: String) {
        // A more robust implementation would check for displayOrder collisions
        taskContextDao.insertContext(TaskContext(name = name))
    }

    suspend fun deleteContext(id: UUID) {
        taskContextDao.deleteContext(id)
    }
}
