package com.ilseon.data.voicememo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "voice_memos")
data class VoiceMemo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    val transcription: String,
    var filePath: String,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)
