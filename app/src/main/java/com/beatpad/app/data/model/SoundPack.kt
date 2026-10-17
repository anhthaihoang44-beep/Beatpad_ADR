package com.beatpad.app.data.model

data class SoundPack(
    val id: String,
    val name: String,
    val genre: String,
    val bpm: Int,
    val key: String,
    val producer: String = "BeatPad Studio",
    val isNew: Boolean = false,
    val colorPrimary: String = "#FFC83D",
    val colorSecondary: String = "#FF8A3D",
    val demoSample: String = "",
    val pads: List<PadInfo>
)
