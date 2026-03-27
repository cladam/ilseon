package com.ilseon.wear.tile

import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService

@OptIn(ExperimentalHorologistApi::class)
class PriorityTaskTileService : SuspendingTileService() {

    companion object {
        private const val RESOURCES_VERSION = "1"
    }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        // Handle button taps — the clickable ID carries the action path
        val lastClickId = requestParams.currentState.lastClickableId
        if (lastClickId.isNotEmpty()) {
            WearTaskDataLoader.sendAction(this, lastClickId)
        }

        val task = WearTaskDataLoader.loadTaskData(this)
        val isRecording = WearTaskDataLoader.loadRecordingState(this)
        val layout = PriorityTaskTileRenderer.buildLayout(task, isRecording, this)

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(15 * 60 * 1000L) // refresh every 15 min as safety net
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(layout)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return PriorityTaskTileRenderer.buildResources(this)
    }
}
