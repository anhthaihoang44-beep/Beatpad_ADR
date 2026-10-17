package com.beatpad.app.ui.loop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatpad.app.data.model.LoopPattern
import com.beatpad.app.data.model.PadInfo
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BorderDark
import com.beatpad.app.ui.theme.SurfaceDark
import com.beatpad.app.ui.theme.TextPrimary
import com.beatpad.app.ui.theme.TextSecondary

@Composable
fun LoopStepGrid(
    pads: List<PadInfo>,
    pattern: LoopPattern,
    currentStep: Int,
    isPlaying: Boolean,
    onStepToggle: (padIndex: Int, step: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF15171C))
    ) {
        // Left Column: Pad Names & Colors (Fixed)
        Column(
            modifier = Modifier
                .width(96.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
        ) {
            // Header spacer to align with step numbers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "PAD",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) {
                items(pads, key = { it.index }) { pad ->
                    PadLabelRow(pad = pad)
                }
            }
        }

        // Right Section: 16 Steps (Horizontally Scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
                .padding(vertical = 4.dp)
        ) {
            // Step Number Header (1..16)
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (step in 0 until LoopPattern.STEP_COUNT) {
                    val isCurrentStep = isPlaying && currentStep == step
                    val isDownbeat = step % 4 == 0

                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isCurrentStep -> AmberAccent
                                    isDownbeat -> Color(0xFF262A34)
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${step + 1}",
                            color = when {
                                isCurrentStep -> Color(0xFF15171C)
                                isDownbeat -> TextPrimary
                                else -> TextSecondary
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isDownbeat || isCurrentStep) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.width(3.dp))
                }
            }

            // Step Grid Rows
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) {
                items(pads, key = { it.index }) { pad ->
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .padding(vertical = 2.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (step in 0 until LoopPattern.STEP_COUNT) {
                            val isActive = pattern.isStepActive(pad.index, step)
                            val isCurrentStep = isPlaying && currentStep == step
                            val isDownbeat = step % 4 == 0

                            StepCell(
                                isActive = isActive,
                                isCurrentStep = isCurrentStep,
                                isDownbeat = isDownbeat,
                                padColor = pad.color,
                                onClick = { onStepToggle(pad.index, step) }
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PadLabelRow(pad: PadInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceDark)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color pill
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(pad.color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${pad.index + 1}. ${pad.name}",
            color = TextPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StepCell(
    isActive: Boolean,
    isCurrentStep: Boolean,
    isDownbeat: Boolean,
    padColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isActive -> padColor
                    isDownbeat -> Color(0xFF222630)
                    else -> Color(0xFF1A1C23)
                }
            )
            .border(
                width = if (isCurrentStep) 2.dp else 1.dp,
                color = when {
                    isCurrentStep -> Color.White
                    isActive -> padColor.copy(alpha = 0.8f)
                    isDownbeat -> BorderDark.copy(alpha = 0.8f)
                    else -> BorderDark.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }
    }
}
