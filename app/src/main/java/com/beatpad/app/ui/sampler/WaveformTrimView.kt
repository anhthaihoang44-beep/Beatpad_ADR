package com.beatpad.app.ui.sampler

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BorderDark
import kotlin.math.abs

@Composable
fun WaveformTrimView(
    samples: FloatArray,
    startTrim: Float,
    endTrim: Float,
    onTrimChanged: (start: Float, end: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingHandle by remember { mutableFloatStateOf(0f) } // -1 for start, 1 for end, 0 for none

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF15171C))
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(samples) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val touchFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val distToStart = abs(touchFraction - startTrim)
                            val distToEnd = abs(touchFraction - endTrim)
                            draggingHandle = if (distToStart < distToEnd) -1f else 1f
                        },
                        onDrag = { change, _ ->
                            val currentFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            if (draggingHandle < 0) {
                                val newStart = minOf(currentFraction, endTrim - 0.05f)
                                onTrimChanged(newStart, endTrim)
                            } else {
                                val newEnd = maxOf(currentFraction, startTrim + 0.05f)
                                onTrimChanged(startTrim, newEnd)
                            }
                        },
                        onDragEnd = { draggingHandle = 0f },
                        onDragCancel = { draggingHandle = 0f }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val midY = height / 2f

            // 1. Dimmed background for unselected regions
            val startX = startTrim * width
            val endX = endTrim * width

            // Highlighted selection box
            drawRect(
                color = AmberAccent.copy(alpha = 0.12f),
                topLeft = Offset(startX, 0f),
                size = Size(endX - startX, height)
            )

            // 2. Draw waveform bars
            if (samples.isNotEmpty()) {
                val barCount = 70
                val samplesPerBar = maxOf(1, samples.size / barCount)
                val barWidth = width / barCount

                for (i in 0 until barCount) {
                    val sIdx = i * samplesPerBar
                    var peak = 0f
                    for (k in 0 until samplesPerBar) {
                        if (sIdx + k < samples.size) {
                            val a = abs(samples[sIdx + k])
                            if (a > peak) peak = a
                        }
                    }
                    val barH = (peak * (height * 0.85f)).coerceAtLeast(3f)
                    val x = i * barWidth + (barWidth * 0.2f)
                    val isInTrim = (i.toFloat() / barCount) in startTrim..endTrim

                    drawLine(
                        color = if (isInTrim) AmberAccent else Color(0xFF4A4E5A),
                        start = Offset(x, midY - barH / 2f),
                        end = Offset(x, midY + barH / 2f),
                        strokeWidth = (barWidth * 0.6f).coerceAtLeast(2f)
                    )
                }
            }

            // 3. Draw Trim Handle Lines
            // Start Handle
            drawLine(
                color = Color.White,
                start = Offset(startX, 0f),
                end = Offset(startX, height),
                strokeWidth = 3.dp.toPx()
            )
            drawCircle(
                color = AmberAccent,
                radius = 6.dp.toPx(),
                center = Offset(startX, midY)
            )

            // End Handle
            drawLine(
                color = Color.White,
                start = Offset(endX, 0f),
                end = Offset(endX, height),
                strokeWidth = 3.dp.toPx()
            )
            drawCircle(
                color = AmberAccent,
                radius = 6.dp.toPx(),
                center = Offset(endX, midY)
            )
        }
    }
}
