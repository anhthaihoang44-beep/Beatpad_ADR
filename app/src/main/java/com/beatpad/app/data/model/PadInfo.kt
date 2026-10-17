package com.beatpad.app.data.model

import androidx.compose.ui.graphics.Color

data class PadInfo(
    val index: Int,
    val name: String,
    val family: InstrumentFamily,
    val fileA: String,
    val fileB: String,
    val color: Color,
    val mode: PadMode
) {
    fun currentFile(side: PackSide): String = if (side == PackSide.SIDE_A) fileA else fileB
}
