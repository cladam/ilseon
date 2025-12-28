package com.ilseon.data.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtractedTasks(
    val tasks: List<TaskInfo>
)

@Serializable
data class TaskInfo(
    val title: String,
    val priority: String,
    val effort: String,
    val type: String? = null,
    val context: String? = null
)
