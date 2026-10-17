package com.beatpad.app.ui.sampler

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.beatpad.app.R
import com.beatpad.app.data.model.InstrumentFamily
import com.beatpad.app.data.model.SoundPack
import com.beatpad.app.data.repository.CustomPackManager
import com.beatpad.app.ui.theme.AmberAccent
import com.beatpad.app.ui.theme.BorderDark
import com.beatpad.app.ui.theme.PadRecordingRed
import com.beatpad.app.ui.theme.SurfaceDark
import com.beatpad.app.ui.theme.TextPrimary
import com.beatpad.app.ui.theme.TextSecondary
import com.beatpad.app.util.AudioRecordSampler
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SamplerDialog(
    customPackManager: CustomPackManager,
    onSampleAssigned: (updatedPack: SoundPack) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionRationale by remember { mutableStateOf(!hasPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        showPermissionRationale = false
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordingElapsedMs by remember { mutableLongStateOf(0L) }
    var recordedSamples by remember { mutableStateOf<FloatArray?>(null) }
    var startTrim by remember { mutableFloatStateOf(0f) }
    var endTrim by remember { mutableFloatStateOf(1f) }

    var selectedPadIndex by remember { mutableIntStateOf(0) }
    var sampleName by remember { mutableStateOf("My Beatbox") }
    var selectedFamily by remember { mutableStateOf(InstrumentFamily.DRUMS) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.mic_sampler_title),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!hasPermission) {
                    Text(
                        text = stringResource(R.string.mic_permission_needed),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.grant_permission), color = Color(0xFF15171C), fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Record / Stop Button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) PadRecordingRed else AmberAccent)
                            .clickable {
                                if (!isRecording) {
                                    isRecording = true
                                    recordingElapsedMs = 0L
                                    scope.launch {
                                        val result = AudioRecordSampler.recordFromMic(
                                            onProgress = { recordingElapsedMs = it }
                                        )
                                        isRecording = false
                                        val samples = result.getOrNull()
                                        if (samples != null && samples.isNotEmpty()) {
                                            recordedSamples = samples
                                            startTrim = 0f
                                            endTrim = 1f
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Record Mic",
                            tint = Color(0xFF15171C),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRecording) "Recording... ${recordingElapsedMs / 1000}s / 3s" else "Tap Mic to record (max 3s)",
                        color = if (isRecording) PadRecordingRed else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Waveform + Trim view
                    val currentSamples = recordedSamples
                    if (currentSamples != null && currentSamples.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        WaveformTrimView(
                            samples = currentSamples,
                            startTrim = startTrim,
                            endTrim = endTrim,
                            onTrimChanged = { s, e ->
                                startTrim = s
                                endTrim = e
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        // Preview Button
                        OutlinedButton(
                            onClick = {
                                val sliced = AudioRecordSampler.sliceAudio(currentSamples, startTrim, endTrim)
                                AudioRecordSampler.playAudioPreview(sliced)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview Trim", color = AmberAccent, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        // Pad Slot Selector (1..12)
                        Text(text = "Assign to Pad:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items((0 until 12).toList()) { idx ->
                                val isSelected = idx == selectedPadIndex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) AmberAccent else Color(0xFF262A34))
                                        .clickable { selectedPadIndex = idx }
                                        .border(1.dp, if (isSelected) Color.White else BorderDark, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        color = if (isSelected) Color(0xFF15171C) else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        // Assign & Save Button
                        Button(
                            onClick = {
                                val sliced = AudioRecordSampler.sliceAudio(currentSamples, startTrim, endTrim)
                                val tempWav = File(context.cacheDir, "temp_sample.wav")
                                AudioRecordSampler.saveToWavFile(sliced, tempWav)
                                scope.launch {
                                    val updated = customPackManager.assignSampleToPad(
                                        padIndex = selectedPadIndex,
                                        sampleFile = tempWav,
                                        padName = sampleName,
                                        family = selectedFamily
                                    )
                                    onSampleAssigned(updated)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Assign to Pad ${selectedPadIndex + 1}", color = Color(0xFF15171C), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        }
    }
}
