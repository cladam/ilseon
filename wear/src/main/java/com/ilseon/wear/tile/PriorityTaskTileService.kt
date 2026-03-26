package com.ilseon.wear.tile

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService

@OptIn(ExperimentalHorologistApi::class)
class PriorityTaskTileService : SuspendingTileService() {

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val deviceParams = requestParams.deviceConfiguration
        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .build()
    }

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
    }
}
