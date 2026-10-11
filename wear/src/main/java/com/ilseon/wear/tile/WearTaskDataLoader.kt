package com.ilseon.wear.tile

import com.ilseon.wear.BuildConfig
import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri

/**
 * Shared helper for loading data from the Wearable DataClient
 * and sending messages to the phone.
 *
 * Recording state is managed with an optimistic override so that
 * Tile, Activity, and Complication all reflect the toggle immediately —
 * before the phone has had time to sync back the real state.
 */
object WearTaskDataLoader {

    private const val TAG = "WearTaskDataLoader"

    // ─── Shared recording state ───────────────────────────────

    private val _recordingState = MutableStateFlow(false)

    /** Observable recording state — collect this in the Activity. */
    val recordingState: StateFlow<Boolean> = _recordingState.asStateFlow()

    // ─── Shared routine state ─────────────────────────────────

    private val _routineState = MutableStateFlow<WearRoutineData?>(null)
    val routineState: StateFlow<WearRoutineData?> = _routineState.asStateFlow()

    /**
     * When non-null, overrides the DataClient value until the phone
     * syncs back the real state (see [clearOptimisticState]).
     */
    private var optimisticRecordingState: Boolean? = null

    /**
     * Debug-only placeholder so the UI is populated when no phone is connected.
     */
    private val DEBUG_PLACEHOLDER_TASK = WearTaskData(
        title = "Morning Flow",
        description = """
            |- Wake up and hydrate
            |- Take a timed shower
            |- Wake up the kids, low-stress and calm
            |- Eat a healthy breakfast
            |- Get dressed and prep for the day
            |- Leave for work
        """.trimMargin(),
        isUrgent = false,
        dueTime = null
    )

    /** Loads the current priority task from the DataClient. */
    suspend fun loadTaskData(context: Context): WearTaskData? {
        return try {
            val dataClient = Wearable.getDataClient(context)
            val dataItems = dataClient.getDataItems(
                "wear://*${WearTaskData.PATH_PRIORITY_TASK}".toUri()
            ).await()

            val item = dataItems.firstOrNull()
            if (item != null) {
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                val descriptionRaw = dataMap.getString(WearTaskData.KEY_DESCRIPTION, "")
                val task = WearTaskData(
                    title = dataMap.getString(WearTaskData.KEY_TITLE, ""),
                    description = descriptionRaw.takeIf { it.isNotEmpty() },
                    isUrgent = dataMap.getBoolean(WearTaskData.KEY_IS_URGENT, false),
                    dueTime = if (dataMap.containsKey(WearTaskData.KEY_DUE_TIME))
                        dataMap.getLong(WearTaskData.KEY_DUE_TIME) else null
                )
                dataItems.release()
                task.takeIf { it.title.isNotEmpty() }
            } else {
                dataItems.release()
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "No task data from phone yet (expected on first run)", e)
            null
        } ?: if (BuildConfig.DEBUG) DEBUG_PLACEHOLDER_TASK else null
    }

    /**
     * Loads the current recording state.
     * Returns the optimistic value if a toggle is pending,
     * otherwise reads from the DataClient and updates the shared flow.
     */
    suspend fun loadRecordingState(context: Context): Boolean {
        val dataClientState = try {
            val dataClient = Wearable.getDataClient(context)
            val dataItems = dataClient.getDataItems(
                "wear://*${WearTaskData.PATH_RECORDING_STATE}".toUri()
            ).await()

            val item = dataItems.firstOrNull()
            val isRecording = if (item != null) {
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                dataMap.getBoolean(WearTaskData.KEY_IS_RECORDING, false)
            } else {
                false
            }
            dataItems.release()
            isRecording
        } catch (e: Exception) {
            Log.d(TAG, "Could not read recording state", e)
            false
        }

        val effective = optimisticRecordingState ?: dataClientState
        _recordingState.value = effective
        return effective
    }

    /**
     * Loads the current routine state from the DataClient.
     */
    suspend fun loadRoutineState(context: Context): WearRoutineData? {
        val state = try {
            val dataClient = Wearable.getDataClient(context)
            val dataItems = dataClient.getDataItems(
                "wear://*${WearTaskData.PATH_ROUTINE_STATE}".toUri()
            ).await()

            val item = dataItems.firstOrNull()
            if (item != null) {
                val dataMap = DataMapItem.fromDataItem(item).dataMap
                val routineTitle = dataMap.getString(WearTaskData.KEY_ROUTINE_TITLE, "")
                val stepTitle = dataMap.getString(WearTaskData.KEY_STEP_TITLE, "")
                val stepIndex = dataMap.getInt(WearTaskData.KEY_STEP_INDEX, 0)
                val totalSteps = dataMap.getInt(WearTaskData.KEY_TOTAL_STEPS, 0)
                val targetDurationMins = if (dataMap.containsKey(WearTaskData.KEY_TARGET_MINS)) {
                    dataMap.getInt(WearTaskData.KEY_TARGET_MINS).takeIf { it > 0 }
                } else null
                val isActive = dataMap.getBoolean(WearTaskData.KEY_IS_ACTIVE, false)
                val isComplete = dataMap.getBoolean(WearTaskData.KEY_IS_COMPLETE, false)

                dataItems.release()
                if (routineTitle.isNotEmpty() && isActive) {
                    WearRoutineData(
                        routineTitle = routineTitle,
                        stepTitle = stepTitle,
                        stepIndex = stepIndex,
                        totalSteps = totalSteps,
                        targetDurationMins = targetDurationMins,
                        isActive = isActive,
                        isComplete = isComplete
                    )
                } else null
            } else {
                dataItems.release()
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not read routine state", e)
            null
        }

        _routineState.value = state
        return state
    }

    /**
     * Sends action to phone to complete the current routine step and advance.
     */
    suspend fun advanceRoutineStep(context: Context) {
        sendAction(context, WearTaskData.ACTION_ROUTINE_NEXT)
    }

    /**
     * Sends action to phone to skip the current routine step.
     */
    suspend fun skipRoutineStep(context: Context) {
        sendAction(context, WearTaskData.ACTION_ROUTINE_SKIP)
    }

    /**
     * Called by [TaskDataListenerService] when routine data arrives from the phone.
     */
    fun updateRoutineStateFromDataMap(dataMap: com.google.android.gms.wearable.DataMap) {
        val routineTitle = dataMap.getString(WearTaskData.KEY_ROUTINE_TITLE, "")
        val stepTitle = dataMap.getString(WearTaskData.KEY_STEP_TITLE, "")
        val stepIndex = dataMap.getInt(WearTaskData.KEY_STEP_INDEX, 0)
        val totalSteps = dataMap.getInt(WearTaskData.KEY_TOTAL_STEPS, 0)
        val targetDurationMins = if (dataMap.containsKey(WearTaskData.KEY_TARGET_MINS)) {
            dataMap.getInt(WearTaskData.KEY_TARGET_MINS).takeIf { it > 0 }
        } else null
        val isActive = dataMap.getBoolean(WearTaskData.KEY_IS_ACTIVE, false)
        val isComplete = dataMap.getBoolean(WearTaskData.KEY_IS_COMPLETE, false)

        if (routineTitle.isNotEmpty() && isActive) {
            _routineState.value = WearRoutineData(
                routineTitle = routineTitle,
                stepTitle = stepTitle,
                stepIndex = stepIndex,
                totalSteps = totalSteps,
                targetDurationMins = targetDurationMins,
                isActive = isActive,
                isComplete = isComplete
            )
        } else {
            _routineState.value = null
        }
    }
    fun clearOptimisticState() {
        Log.d(TAG, "Clearing optimistic recording state")
        optimisticRecordingState = null
    }

    /**
     * Optimistically toggles the recording state and sends the
     * toggle message to the phone. All surfaces (Tile, Activity,
     * Complication) that read from [recordingState] or call
     * [loadRecordingState] will see the new value immediately.
     */
    suspend fun toggleRecording(context: Context) {
        val newState = !_recordingState.value
        optimisticRecordingState = newState
        _recordingState.value = newState
        sendAction(context, WearTaskData.ACTION_TOGGLE_RECORDING)
    }

    /** Sends an action message to all connected phone nodes. */
    suspend fun sendAction(context: Context, actionPath: String) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            Log.d(TAG, "Connected nodes: ${nodes.size} — ${nodes.map { "${it.displayName}(${it.id})" }}")
            if (nodes.isEmpty()) {
                Log.w(TAG, "No connected nodes found — message won't be delivered")
            }
            val messageClient = Wearable.getMessageClient(context)
            for (node in nodes) {
                messageClient.sendMessage(node.id, actionPath, byteArrayOf()).await()
                Log.d(TAG, "Message sent to ${node.displayName}: $actionPath")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send action to phone", e)
        }
    }
}