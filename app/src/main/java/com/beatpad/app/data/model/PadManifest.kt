package com.beatpad.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PadManifest(
    val index: Int = 0,
    val id: Int? = null,
    val name: String,
    val family: InstrumentFamily = InstrumentFamily.DRUMS,
    val file: String? = null,
    val samplePath: String? = null,
    val fileB: String? = null,
    val color: String = "#FF8A3D",
    val mode: PadMode = PadMode.ONE_SHOT,
    val loopBars: Int? = null
) {
    val actualIndex: Int get() = id ?: index

    fun resolveFileA(packId: String): String = when {
        !samplePath.isNullOrEmpty() -> samplePath
        !file.isNullOrEmpty() && file.startsWith("samples/") -> file
        !file.isNullOrEmpty() && file.startsWith("packs/") -> file
        !file.isNullOrEmpty() -> "packs/$packId/$file"
        else -> "packs/$packId/pad_$actualIndex.wav"
    }

    fun resolveFileB(packId: String): String = when {
        !fileB.isNullOrEmpty() && fileB.startsWith("samples/") -> fileB
        !fileB.isNullOrEmpty() && fileB.startsWith("packs/") -> fileB
        !fileB.isNullOrEmpty() -> "packs/$packId/$fileB"
        else -> resolveFileA(packId)
    }
}

@Serializable
data class PackManifest(
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
    val pads: List<PadManifest>
)
