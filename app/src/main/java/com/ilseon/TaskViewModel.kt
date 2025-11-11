package com.ilseon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilseon.data.task.FocusBlock
import com.ilseon.data.task.Task
import com.ilseon.data.task.TaskPriority
import com.ilseon.data.task.TaskRepository
import com.ilseon.data.task.TimerState
import com.ilseon.service.HapticManager
import com.ilseon.service.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val hapticManager: HapticManager,
    private val soundManager: SoundManager
) : ViewModel() {

    val activeFocusBlock: StateFlow<FocusBlock?> = taskRepository.getActiveFocusBlock()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val tasks: StateFlow<List<Task>> = taskRepository.getIncompleteTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onTaskTimerFinished(task: Task) {
        hapticManager.performAlert()
        soundManager.playAlertSound()
        // Here you could also show a notification, etc.
    }

    fun addTask(
        title: String,
        description: String?,
        contextId: UUID?,
        priority: TaskPriority,
        startTimeStr: String,
        endTimeStr: String,
        durationInMinutes: Int?
    ) {
        viewModelScope.launch {
            if (title.isNotBlank() && contextId != null) {
                var startTime: Long? = null
                var endTime: Long? = null
                var duration: Int? = null
                var timerState = TimerState.NotStarted

                if (startTimeStr.isNotBlank() && endTimeStr.isNotBlank()) {
                    val (st, et, dur) = parseTimeAndCalculateDuration(startTimeStr, endTimeStr)
                    startTime = st
                    endTime = et
                    duration = dur
                } else if (durationInMinutes != null) {
                    startTime = System.currentTimeMillis()
                    endTime = startTime + durationInMinutes * 60 * 1000
                    duration = durationInMinutes
                    timerState = TimerState.Running
                }

                taskRepository.insertTask(
                    Task(
                        title = title,
                        description = description,
                        contextId = contextId,
                        priority = priority,
                        startTime = startTime,
                        endTime = endTime,
                        totalTimeInMinutes = duration,
                        timerState = timerState
                    )
                )
            }
        }
    }

    private fun parseTimeAndCalculateDuration(
        startTimeStr: String,
        endTimeStr: String
    ): Triple<Long?, Long?, Int?> {
        if (startTimeStr.isBlank() || endTimeStr.isBlank()) {
            return Triple(null, null, null)
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        try {
            val today = Calendar.getInstance()

            val startCal = Calendar.getInstance()
            startCal.time = timeFormat.parse(startTimeStr) ?: return Triple(null, null, null)
            startCal.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))

            val endCal = Calendar.getInstance()
            endCal.time = timeFormat.parse(endTimeStr) ?: return Triple(null, null, null)
            endCal.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))

            if (endCal.timeInMillis <= startCal.timeInMillis) {
                // Handle overnight or invalid case if necessary
                return Triple(null, null, null)
            }

            val startTime = startCal.timeInMillis
            val endTime = endCal.timeInMillis
            val duration = ((endTime - startTime) / (1000 * 60)).toInt()

            return Triple(startTime, endTime, duration)
        } catch (e: Exception) {
            // Handle parsing exception
            return Triple(null, null, null)
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(
                task.copy(
                    isComplete = true,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
