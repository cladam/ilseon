package com.ilseon.data.task

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReflectionExporter {

    fun exportReflections(tasks: List<Task>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val stringBuilder = StringBuilder()
        stringBuilder.append("Ilseon Task Reflections:\n\n")

        tasks.filter { !it.completionReflection.isNullOrBlank() }
            .forEach { task ->
                stringBuilder.append("Title: ${task.title}\n")
                task.description?.let {
                    stringBuilder.append("Description: $it\n")
                }
                task.completionReflection?.let {
                    stringBuilder.append("Reflection: $it\n")
                }
                stringBuilder.append("Completed: ${dateFormat.format(Date(task.completedAt!!))}\n\n")
            }
        return stringBuilder.toString()
    }

    fun exportAllTasksForDebug(tasks: List<Task>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val stringBuilder = StringBuilder()
        stringBuilder.append("Ilseon All Tasks (Debug Dump):\n\n")

        val sortedTasks = tasks.sortedBy { it.startTime ?: it.createdAt }

        sortedTasks.forEach { task ->
            val status = when {
                task.isComplete -> "COMPLETED"
                (task.startTime ?: 0) > System.currentTimeMillis() -> "FUTURE"
                else -> "INCOMPLETE"
            }
            stringBuilder.append("--- Task: ${task.title} ---\n")
            stringBuilder.append("  Status: $status\n")
            stringBuilder.append("  ID: ${task.id}\n")
            task.startTime?.let {
                stringBuilder.append("  Start Time: ${dateFormat.format(Date(it))}\n")
            }
            task.endTime?.let {
                stringBuilder.append("  End Time:   ${dateFormat.format(Date(it))}\n")
            }
            if (task.isRecurring) {
                stringBuilder.append("  Recurring: Yes (${task.recurrenceDays})\n")
            }
            stringBuilder.append("  Created At: ${dateFormat.format(Date(task.createdAt))}\n")
            task.completedAt?.let {
                stringBuilder.append("  Completed At: ${dateFormat.format(Date(it))}\n")
            }
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }
}