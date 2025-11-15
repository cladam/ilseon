package com.ilseon.data.task

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "focus_blocks",
    foreignKeys = [
        ForeignKey(
            entity = TaskContext::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contextId")]
)
data class FocusBlock(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val contextId: UUID,
    val startTime: String, // Represented as "HH:mm"
    val endTime: String, // Represented as "HH:mm"
    val repeatDays: List<Int> = emptyList()
)
