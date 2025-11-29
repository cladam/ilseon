package com.ilseon.data.task

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a reflection imported from an external source.
 * This serves as an intermediate representation before being converted into a Task entity.
 */
data class ImportedReflection(
    val title: String,
    val description: String?,
    val reflection: String,
    val completedAt: Date
)

/**
 * An interface for parsing reflections from a string input.
 * This allows for multiple parsing strategies for different formats.
 */
interface ReflectionParser {
    fun parse(input: String): Result<List<ImportedReflection>>
}

/**
 * A parser for the default Ilseon reflection export format.
 */
class IlseonReflectionParser : ReflectionParser {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun parse(input: String): Result<List<ImportedReflection>> {
        return try {
            val header = "Ilseon Task Reflections:"
            if (!input.trim().startsWith(header)) {
                return Result.failure(IllegalArgumentException("Input does not appear to be in Ilseon format."))
            }

            val content = input.substringAfter(header).trim()
            if (content.isEmpty()) {
                return Result.success(emptyList())
            }

            val reflections = content.split("\n\n").filter { it.isNotBlank() }.map { block ->
                val lines = block.lines()
                val title = lines.find { it.startsWith("Title:") }?.substringAfter("Title:")?.trim()
                val description = lines.find { it.startsWith("Description:") }?.substringAfter("Description:")?.trim()
                val reflection = lines.find { it.startsWith("Reflection:") }?.substringAfter("Reflection:")?.trim()
                val completedAtString = lines.find { it.startsWith("Completed:") }?.substringAfter("Completed:")?.trim()

                if (title == null || reflection == null || completedAtString == null) {
                    throw IllegalArgumentException("Invalid reflection block found:\n$block")
                }

                val completedAt = dateFormat.parse(completedAtString) ?: throw IllegalArgumentException("Invalid date format in block:\n$block")


                ImportedReflection(
                    title = title,
                    description = description,
                    reflection = reflection,
                    completedAt = completedAt
                )
            }
            Result.success(reflections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Handles the import of reflections, using a specified parser to convert
 * a string input into a list of Task entities.
 */
class ReflectionImporter(private val parser: ReflectionParser) {
    /**
     * Parses the input string and converts the successfully parsed reflections into Task objects.
     *
     * @param input The string containing the reflection data to import.
     * @return A `Result` containing a list of `Task` objects on success, or an exception on failure.
     */
    fun import(input: String, contextId: UUID): Result<List<Task>> {
        return parser.parse(input).map { importedReflections ->
            importedReflections.map { imported ->
                val completedAtTime = imported.completedAt.time
                Task(
                    title = imported.title,
                    description = imported.description,
                    completionReflection = imported.reflection,
                    completedAt = completedAtTime,
                    isComplete = true,
                    createdAt = completedAtTime, // Use completion time as creation time
                    // The following are default values for a newly imported task
                    id = UUID.randomUUID(),
                    startTime = null,
                    endTime = null,
                    isRecurring = false,
                    recurrenceDays = "",
                    priority = TaskPriority.Low,
                    contextId = contextId,
                )
            }
        }
    }
}
