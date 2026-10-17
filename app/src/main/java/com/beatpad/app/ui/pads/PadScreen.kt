package com.beatpad.app.ui.pads

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatpad.app.data.repository.CustomPackManager
import com.beatpad.app.ui.sampler.SamplerDialog
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BackgroundDark
import com.beatpad.app.ui.theme.TextPrimary
import com.beatpad.app.ui.theme.TextSecondary
import com.beatpad.app.util.AppConfig

@Composable
fun PadScreen(
    viewModel: PadViewModel,
    onBack: () -> Unit = {},
    onOpenLoopEditor: () -> Unit = {},
    onOpenTutorial: (packId: String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPreloaded by viewModel.isPreloaded.collectAsStateWithLifecycle()
    val activeLoops by viewModel.activeLoops.collectAsStateWithLifecycle()
    val pendingLoops by viewModel.pendingLoops.collectAsStateWithLifecycle()
    val currentBpm by viewModel.currentBpm.collectAsStateWithLifecycle()
    val isMetronomeActive by viewModel.isMetronomeActive.collectAsStateWithLifecycle()
    val isQuantizeEnabled by viewModel.isQuantizeEnabled.collectAsStateWithLifecycle()
    val currentSide by viewModel.currentSide.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val customPackManager = remember { CustomPackManager(context) }
    var showSamplerDialog by remember { mutableStateOf(false) }

    val pack = uiState.pack

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top Bar: Back + Mic, Pack title & genre, Settings + Tutorial
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { showSamplerDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Beatbox Sampler",
                        tint = AmberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = pack?.name ?: AppConfig.APP_NAME,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (pack != null) "${pack.genre} • Side ${currentSide.name.last()}" else "Loading...",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { onOpenTutorial(pack?.id ?: "neon_trap") },
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Tutorial",
                        color = AmberAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Control Row: BPM chip, Record, Side A/B, LOOP, Quantize, Metronome
        if (pack != null) {
            ControlRow(
                currentBpm = currentBpm,
                basePackBpm = pack.bpm,
                currentSide = currentSide,
                isMetronomeActive = isMetronomeActive,
                isQuantizeEnabled = isQuantizeEnabled,
                isRecording = uiState.isRecording,
                recordingDurationMs = uiState.recordingDurationMs,
                onBpmSelected = { viewModel.setBpm(it) },
                onSideToggle = { viewModel.toggleSide() },
                onMetronomeToggle = { viewModel.toggleMetronome() },
                onQuantizeToggle = { viewModel.setQuantizeEnabled(!isQuantizeEnabled) },
                onRecordToggle = { viewModel.toggleRecord() },
                onLoopEditorClick = onOpenLoopEditor
            )
        }

        // Loading indicator or 3x4 Pad Grid
        if (uiState.isLoading || !isPreloaded || pack == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AmberAccent, modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Loading audio pack...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            PadGrid(
                pads = pack.pads,
                currentSide = currentSide,
                pressedPadIds = uiState.pressedPadIds,
                activeLoops = activeLoops,
                pendingLoops = pendingLoops,
                onPadDown = { viewModel.onPadDown(it) },
                onPadUp = { viewModel.onPadUp(it) },
                modifier = Modifier.weight(1.0f)
            )
        }

        // Phase 4: Save Recording Dialog
        if (uiState.showSaveDialog) {
            SaveRecordingDialog(
                defaultName = uiState.defaultTrackName,
                durationMs = uiState.recordingDurationMs,
                eventCount = uiState.recordedEventCount,
                onSave = { name -> viewModel.saveTrack(name) },
                onDiscard = { viewModel.discardRecording() }
            )
        }

        // Phase 4: Offline WAV Rendering Progress Dialog
        if (uiState.isRendering) {
            RenderingProgressDialog(progress = uiState.renderProgress)
        }

        // Phase 6: Beatbox Sampler Dialog
        if (showSamplerDialog) {
            SamplerDialog(
                customPackManager = customPackManager,
                onSampleAssigned = { updatedPack ->
                    viewModel.loadPack(updatedPack.id)
                },
                onDismiss = { showSamplerDialog = false }
            )
        }
    }
}
