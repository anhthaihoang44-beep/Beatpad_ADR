package com.beatpad.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RecordingEvent(
    val padIndex: Int,
    val pressTimeMs: Long,
    val releaseTimeMs: Long = 0L,
    val side: String = "SIDE_A",
    val bpm: Int = 140
)
