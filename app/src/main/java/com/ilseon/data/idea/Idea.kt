package com.ilseon.data.idea

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * This is the Idea Entity
 * Capture a quick idea or thought
 */
@Entity
data class Idea(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val content: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isConverted: Boolean = false
)