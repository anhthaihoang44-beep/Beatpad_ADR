package com.beatpad.app.ui.pads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpad.app.data.model.PackSide
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BorderDark
import com.beatpad.app.ui.theme.PadRecordingRed
import com.beatpad.app.ui.theme.SurfaceDark
import com.beatpad.app.ui.theme.TextPrimary
import com.beatpad.app.ui.theme.TextSecondary

@Composable
fun ControlRow(
    currentBpm: Int,
    basePackBpm: Int,
    currentSide: PackSide,
    isMetronomeActive: Boolean,
    isQuantizeEnabled: Boolean,
    isRecording: Boolean = false,
    recordingDurationMs: Long = 0L,
    onBpmSelected: (Int) -> Unit,
    onSideToggle: () -> Unit,
    onMetronomeToggle: () -> Unit,
    onQuantizeToggle: () -> Unit,
    onRecordToggle: () -> Unit = {},
    onLoopEditorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isBpmExpanded by remember { mutableStateOf(false) }
    val recordTransition = rememberInfiniteTransition(label = "recPulse")
    val recPulseScale by recordTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. BPM Chip
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isBpmExpanded) AmberAccent.copy(alpha = 0.18f) else Color(0xFF262A34))
                    .clickable { isBpmExpanded = !isBpmExpanded }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "BPM",
                    tint = if (isBpmExpanded) AmberAccent else TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$currentBpm",
                    color = if (isBpmExpanded) AmberAccent else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2. Active Record Button & Timer
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRecording) PadRecordingRed.copy(alpha = 0.2f) else Color(0xFF262A34))
                    .border(1.dp, if (isRecording) PadRecordingRed else BorderDark, RoundedCornerShape(8.dp))
                    .clickable { onRecordToggle() }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "Record",
                    tint = if (isRecording) PadRecordingRed else Color.Red.copy(alpha = 0.85f),
                    modifier = Modifier.size(13.dp).scale(if (isRecording) recPulseScale else 1.0f)
                )
                if (isRecording) {
                    Spacer(modifier = Modifier.width(3.dp))
                    val recSec = (recordingDurationMs / 1000) % 60
                    val recMin = (recordingDurationMs / 1000) / 60
                    Text(
                        text = String.format("%02d:%02d", recMin, recSec),
                        color = PadRecordingRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Side A / B Segmented Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2129))
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (currentSide == PackSide.SIDE_A) AmberAccent else Color.Transparent)
                        .clickable { if (currentSide != PackSide.SIDE_A) onSideToggle() }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        color = if (currentSide == PackSide.SIDE_A) Color(0xFF15171C) else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (currentSide == PackSide.SIDE_B) AmberAccent else Color.Transparent)
                        .clickable { if (currentSide != PackSide.SIDE_B) onSideToggle() }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        color = if (currentSide == PackSide.SIDE_B) Color(0xFF15171C) else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // 4. Loop Sequencer Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262A34))
                    .border(1.dp, AmberAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { onLoopEditorClick() }
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LOOP",
                    color = AmberAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 5. Quantize Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isQuantizeEnabled) AmberAccent.copy(alpha = 0.15f) else Color(0xFF262A34))
                    .clickable { onQuantizeToggle() }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SYNC",
                    color = if (isQuantizeEnabled) AmberAccent else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 6. Metronome Toggle
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isMetronomeActive) AmberAccent else Color(0xFF262A34))
                    .clickable { onMetronomeToggle() }
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Metronome",
                    tint = if (isMetronomeActive) Color(0xFF15171C) else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Expandable BPM Picker Strip
        AnimatedVisibility(
            visible = isBpmExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val minBpm = (basePackBpm - 20).coerceAtLeast(40)
            val maxBpm = (basePackBpm + 20).coerceAtMost(240)
            val bpmList = remember(basePackBpm) { (minBpm..maxBpm).toList() }
            val listState = rememberLazyListState()

            LaunchedEffect(isBpmExpanded) {
                val targetIndex = (currentBpm - minBpm).coerceIn(0, bpmList.size - 1)
                listState.scrollToItem((targetIndex - 2).coerceAtLeast(0))
            }

            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(bpmList) { bpm ->
                    val isSelected = bpm == currentBpm
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AmberAccent else Color(0xFF262A34))
                            .clickable { onBpmSelected(bpm) }
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bpm.toString(),
                            color = if (isSelected) Color(0xFF15171C) else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
