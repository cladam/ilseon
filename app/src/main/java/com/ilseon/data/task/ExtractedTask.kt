package com.ilseon.data.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtractedTasks(
    @SerialName("extracted_tasks")
    val tasks: List<TaskInfo>
)

@Serializable
data class TaskInfo(
    val title: String,
    val effort: String,
    val type: String,
    val context: String? = null
)
