package com.ilseon.data.idea

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IdeaExporter {

    fun exportIdeas(ideas: List<Idea>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val stringBuilder = StringBuilder()
        stringBuilder.append("### Ilseon Ideas and Notes ###\n\n")

        ideas.forEach { idea ->
            // If it's converted then it's archived
            if (idea.isConverted) {
                return@forEach
            }
            if (idea.isReference) {
                stringBuilder.append("Note: \n")
            } else {
                stringBuilder.append("Idea: \n")
            }
            idea.content?.let { stringBuilder.append("$it\n") }
            stringBuilder.append("CreatedAt: ${dateFormat.format(Date(idea.createdAt))}\n")
            stringBuilder.append("---\n")
        }
        return stringBuilder.toString()
    }

    fun exportIdeasForDebug(ideas: List<Idea>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val stringBuilder = StringBuilder()
        stringBuilder.append("### Ilseon Ideas Export ###\n\n")

        ideas.forEach { idea ->
            stringBuilder.append("--- IDEA START ---\n")
            stringBuilder.append("ID: ${idea.id}\n")
            stringBuilder.append("CreatedAt: ${dateFormat.format(Date(idea.createdAt))}\n")
            stringBuilder.append("IsReference: ${idea.isReference}\n")
            stringBuilder.append("IsPinned: ${idea.isPinned}\n")
            stringBuilder.append("Weight: ${idea.weight}\n")
            idea.contextId?.let { stringBuilder.append("ContextID: $it\n") }
            if (idea.imageUris.isNotEmpty()) {
                stringBuilder.append("ImageURIs: ${idea.imageUris.joinToString(",")}\n")
            }
            stringBuilder.append("IsConverted: ${idea.isConverted}\n")
            stringBuilder.append("--- CONTENT START ---\n")
            idea.content?.let { stringBuilder.append(it) }
            stringBuilder.append("\n--- CONTENT END ---\n")
            stringBuilder.append("--- IDEA END ---\n\n")
        }
        return stringBuilder.toString()
    }
}
