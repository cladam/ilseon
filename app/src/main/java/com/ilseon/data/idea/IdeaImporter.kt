package com.ilseon.data.idea

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents an idea/note imported from the user-friendly text format.
 * This serves as an intermediate representation before being converted into an Idea entity.
 */
data class ImportedIdea(
    val content: String,
    val isReference: Boolean,
    val createdAt: Date
)

/**
 * An interface for parsing ideas from a string input.
 * This allows for multiple parsing strategies for different formats.
 */
interface IdeaParser {
    fun parse(input: String): Result<List<ImportedIdea>>
}

/**
 * A parser for the user-friendly Ilseon idea and note export format.
 */
class IlseonIdeaParser : IdeaParser {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun parse(input: String): Result<List<ImportedIdea>> {
        return try {
            val header = "### Ilseon Ideas and Notes ###"
            if (!input.trim().startsWith(header)) {
                return Result.failure(IllegalArgumentException("Input does not appear to be in Ilseon format."))
            }

            val content = input.substringAfter(header).trim()
            if (content.isEmpty()) {
                return Result.success(emptyList())
            }

            // Split by "---" delimiter used in export
            val ideaBlocks = content.split("---").filter { it.isNotBlank() }

            val importedIdeas = ideaBlocks.map { block ->
                val lines = block.trim().lines().filter { it.isNotBlank() }

                val typeLine = lines.firstOrNull { it.startsWith("Note:") || it.startsWith("Idea:") }
                    ?: throw IllegalArgumentException("Missing Idea/Note type in block:\n$block")

                val isReference = typeLine.startsWith("Note:")

                val createdAtLine = lines.firstOrNull { it.startsWith("CreatedAt:") }
                    ?: throw IllegalArgumentException("Missing CreatedAt in block:\n$block")

                val createdAtString = createdAtLine.substringAfter("CreatedAt:").trim()
                val createdAt = dateFormat.parse(createdAtString)
                    ?: throw IllegalArgumentException("Invalid date format in block:\n$block")

                // Content is everything between type line and CreatedAt line
                val typeLineIndex = lines.indexOf(typeLine)
                val createdAtIndex = lines.indexOf(createdAtLine)
                val ideaContent = if (createdAtIndex > typeLineIndex + 1) {
                    lines.subList(typeLineIndex + 1, createdAtIndex).joinToString("\n")
                } else {
                    ""
                }

                ImportedIdea(
                    content = ideaContent,
                    isReference = isReference,
                    createdAt = createdAt
                )
            }
            Result.success(importedIdeas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}

/**
 * Handles the import of ideas, using a specified parser to convert
 * a string input into a list of Idea entities.
 */
class IdeaImporter(private val parser: IdeaParser) {
    /**
     * Parses the input string and converts the successfully parsed ideas into Idea objects.
     *
     * @param input The string containing the idea data to import.
     * @return A `Result` containing a list of `Idea` objects on success, or an exception on failure.
     */
    fun import(input: String, importedContextId: UUID): Result<List<Idea>> {
        return parser.parse(input).map { importedIdeas ->
            importedIdeas.map { imported ->
                Idea(
                    id = UUID.randomUUID(),
                    content = imported.content,
                    isReference = imported.isReference,
                    createdAt = imported.createdAt.time,
                    imageUris = emptyList(),
                    isConverted = false,
                    isPinned = false,
                    weight = 0,
                    contextId = importedContextId // Use imported context
                )
            }
        }
    }
}
