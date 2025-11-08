package com.ilseon.data.task

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TaskRepository(private val taskDao: TaskDao) {
    fun getIncompleteTasks(): Flow<List<Task>> {
        return taskDao.getIncompleteTasks()
    }

    fun getTasks(): Flow<List<Task>> = taskDao.getTasks()

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun insert(task: Task) {
        taskDao.insert(task)
    }

    suspend fun update(task: Task) {
        taskDao.update(task)
    }


    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun getTaskById(id: UUID): Task? {
        return taskDao.getTaskById(id)
    }
}
