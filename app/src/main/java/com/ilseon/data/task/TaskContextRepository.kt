package com.ilseon.data.task

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskContextRepository @Inject constructor(
    private val taskContextDao: TaskContextDao,
    private val focusBlockDao: FocusBlockDao
) {

    fun getContexts(): Flow<List<TaskContext>> = taskContextDao.getContexts()

    suspend fun addContext(
        name: String,
        description: String?,
        startTime: String?,
        endTime: String?,
        repeatDays: List<Int>?
    ) {
        val newContext = TaskContext(name = name, description = description)
        taskContextDao.insertContext(newContext)

        if (startTime != null && endTime != null && startTime.isNotBlank() && endTime.isNotBlank()) {
            val focusBlock = FocusBlock(
                contextId = newContext.id,
                startTime = startTime,
                endTime = endTime,
                repeatDays = repeatDays ?: emptyList()
            )
            focusBlockDao.insert(focusBlock)
        }
    }

    suspend fun updateContext(
        id: UUID,
        name: String,
        description: String?,
        startTime: String?,
        endTime: String?,
        repeatDays: List<Int>?
    ) {
        val context = taskContextDao.getContext(id)
        if (context != null) {
            val updatedContext = context.copy(name = name, description = description)
            taskContextDao.updateContext(updatedContext)

            val focusBlock = focusBlockDao.getFocusBlockForContext(id)
            if (startTime != null && endTime != null && startTime.isNotBlank() && endTime.isNotBlank()) {
                if (focusBlock != null) {
                    val updatedFocusBlock = focusBlock.copy(
                        startTime = startTime,
                        endTime = endTime,
                        repeatDays = repeatDays ?: emptyList()
                    )
                    focusBlockDao.update(updatedFocusBlock)
                } else {
                    val newFocusBlock = FocusBlock(
                        contextId = id,
                        startTime = startTime,
                        endTime = endTime,
                        repeatDays = repeatDays ?: emptyList()
                    )
                    focusBlockDao.insert(newFocusBlock)
                }
            } else {
                if (focusBlock != null) {
                    focusBlockDao.delete(focusBlock)
                }
            }
        }
    }

    suspend fun deleteContext(id: UUID) {
        taskContextDao.deleteContext(id)
    }
}