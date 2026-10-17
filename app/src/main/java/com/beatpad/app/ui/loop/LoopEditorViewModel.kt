package com.beatpad.app.ui.loop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beatpad.app.audio.AudioEngine
import com.beatpad.app.audio.AudioRenderer
import com.beatpad.app.audio.StepTimingUtils
import com.beatpad.app.data.local.BeatPadDatabase
import com.beatpad.app.data.local.TrackEntity
import com.beatpad.app.data.model.LoopPattern
import com.beatpad.app.data.model.SoundPack
import com.beatpad.app.data.repository.RoomTrackRepository
import com.beatpad.app.data.repository.TrackRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

data class LoopEditorUiState(
    val pack: SoundPack,
    val pattern: LoopPattern,
    val isPlaying: Boolean = false,
    val currentStep: Int = 0,
    val isMetronomeActive: Boolean = false,
    val isRendering: Boolean = false,
    val renderProgress: Float = 0.0f,
    val saveMessage: String? = null
)

class LoopEditorViewModel(
    application: Application,
    val pack: SoundPack,
    private val audioEngine: AudioEngine,
    private val trackRepository: TrackRepository = RoomTrackRepository(
        BeatPadDatabase.getDatabase(application).trackDao()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        LoopEditorUiState(
            pack = pack,
            pattern = LoopPattern.empty(pack.id, pack.bpm),
            isMetronomeActive = false
        )
    )
    val uiState: StateFlow<LoopEditorUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null

    fun toggleStep(padIndex: Int, step: Int) {
        val updatedPattern = _uiState.value.pattern.toggleStep(padIndex, step)
        _uiState.value = _uiState.value.copy(pattern = updatedPattern)
    }

    fun clearPattern() {
        val cleared = _uiState.value.pattern.clear()
        _uiState.value = _uiState.value.copy(pattern = cleared)
    }

    fun setBpm(newBpm: Int) {
        val updatedPattern = _uiState.value.pattern.copy(bpm = newBpm)
        _uiState.value = _uiState.value.copy(pattern = updatedPattern)
        audioEngine.setBpm(newBpm)
    }

    fun setSwing(swingPercent: Int) {
        val updatedPattern = _uiState.value.pattern.copy(swingPercent = swingPercent)
        _uiState.value = _uiState.value.copy(pattern = updatedPattern)
    }

    fun toggleMetronome() {
        val newMetro = !_uiState.value.isMetronomeActive
        _uiState.value = _uiState.value.copy(isMetronomeActive = newMetro)
    }

    fun togglePlay() {
        if (_uiState.value.isPlaying) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        _uiState.value = _uiState.value.copy(isPlaying = true, currentStep = 0)

        playbackJob = viewModelScope.launch {
            var step = 0
            while (isActive && _uiState.value.isPlaying) {
                _uiState.value = _uiState.value.copy(currentStep = step)
                val currentPattern = _uiState.value.pattern

                // Trigger active pads for this step
                for (padIndex in 0 until LoopPattern.PAD_COUNT) {
                    if (currentPattern.isStepActive(padIndex, step)) {
                        audioEngine.onPadDown(padIndex)
                    }
                }

                // Calculate duration to next step (including swing delay)
                val baseDurationMs = StepTimingUtils.calculateStepDurationMs(currentPattern.bpm)
                val swingOffsetMs = StepTimingUtils.calculateSwingOffsetMs(
                    stepIndex = step,
                    stepDurationMs = baseDurationMs,
                    swingPercent = currentPattern.swingPercent
                )
                val delayTime = (baseDurationMs + swingOffsetMs).toLong().coerceAtLeast(10L)

                delay(delayTime)
                step = (step + 1) % LoopPattern.STEP_COUNT
            }
        }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.value = _uiState.value.copy(isPlaying = false, currentStep = 0)
    }

    fun saveAsTrack() {
        val currentPattern = _uiState.value.pattern
        if (!currentPattern.hasAnyActiveSteps()) {
            _uiState.value = _uiState.value.copy(saveMessage = "Grid is empty. Add some beats first!")
            return
        }

        stopPlayback()
        _uiState.value = _uiState.value.copy(isRendering = true, renderProgress = 0.05f)

        viewModelScope.launch {
            val events = StepTimingUtils.generateEventsForBars(currentPattern, totalBars = 4)
            val stepDurationMs = StepTimingUtils.calculateStepDurationMs(currentPattern.bpm)
            val durationMs = (stepDurationMs * LoopPattern.STEP_COUNT * 4).toLong()

            val context = getApplication<Application>()
            val recDir = File(context.filesDir, "recordings")
            recDir.mkdirs()
            val wavFile = File(recDir, "loop_${UUID.randomUUID()}.wav")

            val renderResult = AudioRenderer.renderMixToWav(
                context = context,
                pack = pack,
                events = events,
                destinationFile = wavFile,
                recordingDurationMs = durationMs,
                onProgress = { p ->
                    _uiState.value = _uiState.value.copy(renderProgress = p)
                }
            )

            renderResult.onSuccess { file ->
                val trackName = "${pack.name} 4-Bar Loop"
                val track = TrackEntity(
                    name = trackName,
                    packId = pack.id,
                    createdAt = System.currentTimeMillis(),
                    durationMs = durationMs.coerceAtLeast(1000L),
                    filePath = file.absolutePath,
                    eventsJson = Json.encodeToString(events)
                )
                trackRepository.insertTrack(track)
                _uiState.value = _uiState.value.copy(
                    isRendering = false,
                    saveMessage = "Saved 4 bars to My Tracks!"
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isRendering = false,
                    saveMessage = "Failed to render WAV audio."
                )
            }
        }
    }

    fun dismissSaveMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }

    class Factory(
        private val application: Application,
        private val pack: SoundPack,
        private val audioEngine: AudioEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoopEditorViewModel(application, pack, audioEngine) as T
        }
    }
}
