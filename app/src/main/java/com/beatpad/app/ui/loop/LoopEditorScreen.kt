package com.beatpad.app.ui.loop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatpad.app.R
import com.beatpad.app.ui.pads.RenderingProgressDialog
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BackgroundDark
import com.beatpad.app.ui.theme.BorderDark
import com.beatpad.app.ui.theme.SurfaceDark
import com.beatpad.app.ui.theme.TextPrimary
import com.beatpad.app.ui.theme.TextSecondary

@Composable
fun LoopEditorScreen(
    viewModel: LoopEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pack = uiState.pack
    var isBpmExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top Bar: Back, Title, Save Track Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.mode_sequencer),
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${pack.name} • ${uiState.pattern.bpm} BPM",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            // Save 4-Bar Track Button
            IconButton(
                onClick = { viewModel.saveAsTrack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save as track",
                    tint = AmberAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Horizontal BPM Strip & Toggle Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isBpmExpanded) AmberAccent.copy(alpha = 0.2f) else SurfaceDark)
                    .clickable { isBpmExpanded = !isBpmExpanded }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "BPM: ${uiState.pattern.bpm} (Tap to adjust)",
                    color = if (isBpmExpanded) AmberAccent else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "16 Steps / 4 Beats",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // Expandable BPM Picker Strip
        AnimatedVisibility(visible = isBpmExpanded) {
            val minBpm = (pack.bpm - 20).coerceAtLeast(40)
            val maxBpm = (pack.bpm + 20).coerceAtMost(240)
            val bpmList = remember(pack.bpm) { (minBpm..maxBpm).toList() }
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (bpm in bpmList) {
                    val isSelected = bpm == uiState.pattern.bpm
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) AmberAccent else SurfaceDark)
                            .clickable { viewModel.setBpm(bpm) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$bpm",
                            color = if (isSelected) Color(0xFF15171C) else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Center: Step Grid (16 Steps x 12 Pads)
        Box(modifier = Modifier.weight(1.0f)) {
            LoopStepGrid(
                pads = pack.pads,
                pattern = uiState.pattern,
                currentStep = uiState.currentStep,
                isPlaying = uiState.isPlaying,
                onStepToggle = { padIndex, step ->
                    viewModel.toggleStep(padIndex, step)
                }
            )
        }

        // Bottom: Control Bar (Play/Stop, Clear, Metronome, Swing)
        LoopControlBar(
            isPlaying = uiState.isPlaying,
            isMetronomeActive = uiState.isMetronomeActive,
            swingPercent = uiState.pattern.swingPercent,
            onPlayToggle = { viewModel.togglePlay() },
            onClear = { viewModel.clearPattern() },
            onMetronomeToggle = { viewModel.toggleMetronome() },
            onSwingChange = { viewModel.setSwing(it) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // Rendering Progress Dialog
        if (uiState.isRendering) {
            RenderingProgressDialog(progress = uiState.renderProgress)
        }

        // Save Message Dialog
        uiState.saveMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSaveMessage() },
                containerColor = SurfaceDark,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = { Text("Loop Sequencer", fontWeight = FontWeight.Bold) },
                text = { Text(msg) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissSaveMessage() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                    ) {
                        Text(stringResource(R.string.continue_btn), color = Color(0xFF15171C))
                    }
                }
            )
        }
    }
}
