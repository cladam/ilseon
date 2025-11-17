package com.ilseon.data.task

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteExporter {

    fun exportNotes(tasks: List<Task>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val stringBuilder = StringBuilder()
        stringBuilder.append("Ilseon Tasks:\n\n")

        tasks.filter { !it.completionReflection.isNullOrBlank() }
            .forEach { task ->
                stringBuilder.append("Title: ${task.title}\n")
                task.description?.let {
                    stringBuilder.append("Description: $it\n")
                }
                task.completionReflection?.let {
                    stringBuilder.append("Reflection: $it\n")
                }
                stringBuilder.append("Created: ${dateFormat.format(Date(task.createdAt))}\n\n")
            }
        return stringBuilder.toString()
    }
}
