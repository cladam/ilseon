package com.ilseon.di

import com.ilseon.data.task.TaskRepository
import com.ilseon.service.HapticManager
import com.ilseon.service.NotificationService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun taskRepository(): TaskRepository
    fun notificationService(): NotificationService
    fun hapticManager(): HapticManager
}

