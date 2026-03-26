package com.ilseon.wear.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class StartRecordingComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("Record").build(),
            contentDescription = PlainComplicationText.Builder("Start recording a voice memo").build()
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        // For now, we'll return a simple complication. We'll add the communication
        // with the phone app later.
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("Record").build(),
            contentDescription = PlainComplicationText.Builder("Start recording a voice memo").build()
        ).build()
    }
}
