package com.beatpad.app.ui.tracks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BorderDark

@Composable
fun WaveformThumbnail(
    bars: List<Float>,
    progress: Float = 0.0f,
    modifier: Modifier = Modifier,
    activeColor: Color = AmberAccent,
    inactiveColor: Color = Color(0xFF323642)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        if (bars.isEmpty()) return@Canvas

        val barCount = bars.size
        val totalSpacing = (barCount - 1) * 2.dp.toPx()
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(1.5f)
        val spacing = 2.dp.toPx()
        val middleY = size.height / 2f
        val currentActiveIndex = (progress * barCount).toInt().coerceIn(0, barCount)

        for (i in 0 until barCount) {
            val barAmp = bars[i].coerceIn(0.1f, 1.0f)
            val barHeight = (size.height * 0.85f * barAmp).coerceAtLeast(4.dp.toPx())
            val left = i * (barWidth + spacing)
            val top = middleY - (barHeight / 2f)

            val color = if (i <= currentActiveIndex && progress > 0.001f) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
