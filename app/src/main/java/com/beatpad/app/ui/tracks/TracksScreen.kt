package com.beatpad.app.ui.tracks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beatpad.app.data.local.TrackEntity
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BackgroundDark
import com.beatpad.app.ui.theme.BorderDark
import com.beatpad.app.ui.theme.PadRecordingRed
import com.beatpad.app.ui.theme.SurfaceDark
import com.beatpad.app.ui.theme.TextPrimary
import com.beatpad.app.ui.theme.TextSecondary

@Composable
fun TracksScreen(
    viewModel: TracksViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val playingTrackId by viewModel.playingTrackId.collectAsStateWithLifecycle()
    val progress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val waveforms by viewModel.waveformsState.collectAsStateWithLifecycle()

    var trackToRename by remember { mutableStateOf<TrackEntity?>(null) }
    var trackToDelete by remember { mutableStateOf<TrackEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = AmberAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Column {
                Text(
                    text = "My Tracks",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (tracks.isEmpty()) "No saved recordings" else "${tracks.size} recordings",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        if (tracks.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = PadRecordingRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Recordings Yet",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the Red Record button on the pads to record your live beat sessions to lossless WAV.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            // Tracks List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    LaunchedEffect(track.id) {
                        viewModel.loadWaveform(track)
                    }

                    val isPlaying = playingTrackId == track.id
                    val waveformBars = waveforms[track.id] ?: List(40) { 0.15f }

                    TrackCard(
                        track = track,
                        isPlaying = isPlaying,
                        progress = if (isPlaying) progress else 0.0f,
                        waveformBars = waveformBars,
                        onPlayToggle = { viewModel.togglePlay(track) },
                        onRenameClick = { trackToRename = track },
                        onDeleteClick = { trackToDelete = track },
                        onShareClick = { viewModel.shareTrack(context, track) }
                    )
                }
            }
        }
    }

    // Rename Dialog
    trackToRename?.let { track ->
        RenameTrackDialog(
            initialName = track.name,
            onConfirm = { newName ->
                viewModel.renameTrack(track.id, newName)
                trackToRename = null
            },
            onDismiss = { trackToRename = null }
        )
    }

    // Delete Dialog
    trackToDelete?.let { track ->
        DeleteTrackDialog(
            trackName = track.name,
            onConfirm = {
                viewModel.deleteTrack(track)
                trackToDelete = null
            },
            onDismiss = { trackToDelete = null }
        )
    }
}
