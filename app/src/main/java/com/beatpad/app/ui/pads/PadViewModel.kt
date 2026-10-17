package com.beatpad.app.ui.pads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beatpad.app.audio.AudioEngine
import com.beatpad.app.audio.AudioRenderer
import com.beatpad.app.audio.SoundPoolAudioEngine
import com.beatpad.app.data.local.BeatPadDatabase
import com.beatpad.app.data.local.TrackEntity
import com.beatpad.app.data.model.PackSide
import com.beatpad.app.data.model.RecordingEvent
import com.beatpad.app.data.model.SoundPack
import com.beatpad.app.data.repository.AssetPackRepository
import com.beatpad.app.data.repository.PackRepository
import com.beatpad.app.data.repository.RoomTrackRepository
import com.beatpad.app.data.repository.TrackRepository
import com.beatpad.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

data class PadUiState(
    val pack: SoundPack? = null,
    val pressedPadIds: Set<Int> = emptySet(),
    val isLoading: Boolean = true,
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val showSaveDialog: Boolean = false,
    val recordedEventCount: Int = 0,
    val defaultTrackName: String = "",
    val isRendering: Boolean = false,
    val renderProgress: Float = 0.0f
)

class PadViewModel(
    application: Application,
    private val packRepository: PackRepository = AssetPackRepository(application),
    private val preferencesRepository: UserPreferencesRepository = UserPreferencesRepository(application),
    private val trackRepository: TrackRepository = RoomTrackRepository(BeatPadDatabase.getDatabase(application).trackDao()),
    private val audioEngine: AudioEngine = SoundPoolAudioEngine(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PadUiState())
    val uiState: StateFlow<PadUiState> = _uiState.asStateFlow()

    val engine: AudioEngine get() = audioEngine
    val repository: PackRepository get() = packRepository

    val isPreloaded: StateFlow<Boolean> = audioEngine.isPreloaded
    val activeLoops: StateFlow<Set<Int>> = audioEngine.activeLoops
    val pendingLoops: StateFlow<Set<Int>> = audioEngine.pendingLoops
    val currentBpm: StateFlow<Int> = audioEngine.currentBpm
    val isMetronomeActive: StateFlow<Boolean> = audioEngine.isMetronomeActive
    val isQuantizeEnabled: StateFlow<Boolean> = audioEngine.isQuantizeEnabled
    val currentSide: StateFlow<PackSide> = audioEngine.currentSide

    private val recordedEvents = mutableListOf<RecordingEvent>()
    private var recordStartTimeMs: Long = 0L
    private var recordingTimerJob: Job? = null

    private var isHapticsEnabled = true
    private val vibrator = application.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator

    init {
        viewModelScope.launch {
            preferencesRepository.hapticsEnabled.collect { isHapticsEnabled = it }
        }
        viewModelScope.launch {
            val initialPackId = preferencesRepository.lastPackId.first()
            loadPack(initialPackId)
        }
    }

    fun loadPack(packId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            audioEngine.stopAll()
            val result = packRepository.getPack(packId)
            val pack = result.getOrNull()
            if (pack != null) {
                _uiState.value = _uiState.value.copy(pack = pack)
                audioEngine.preload(pack)
                preferencesRepository.setLastPackId(packId)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onPadDown(index: Int) {
        val state = _uiState.value
        _uiState.value = state.copy(pressedPadIds = state.pressedPadIds + index)
        audioEngine.onPadDown(index)

        if (isHapticsEnabled) {
            runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(15, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator?.vibrate(15)
                }
            }
        }

        if (_uiState.value.isRecording) {
            val pressTime = System.currentTimeMillis() - recordStartTimeMs
            val event = RecordingEvent(
                padIndex = index,
                pressTimeMs = pressTime,
                side = currentSide.value.name,
                bpm = currentBpm.value
            )
            recordedEvents.add(event)
            _uiState.value = _uiState.value.copy(recordedEventCount = recordedEvents.size)
        }
    }

    fun onPadUp(index: Int) {
        val state = _uiState.value
        _uiState.value = state.copy(pressedPadIds = state.pressedPadIds - index)
        audioEngine.onPadUp(index)

        if (_uiState.value.isRecording) {
            val releaseTime = System.currentTimeMillis() - recordStartTimeMs
            val lastIdx = recordedEvents.indexOfLast { it.padIndex == index && it.releaseTimeMs == 0L }
            if (lastIdx != -1) {
                recordedEvents[lastIdx] = recordedEvents[lastIdx].copy(releaseTimeMs = releaseTime)
            }
        }
    }

    fun toggleRecord() {
        if (!_uiState.value.isRecording) {
            // Start recording
            recordedEvents.clear()
            recordStartTimeMs = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                isRecording = true,
                recordingDurationMs = 0L,
                recordedEventCount = 0,
                showSaveDialog = false
            )
            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                while (isActive) {
                    delay(100L)
                    _uiState.value = _uiState.value.copy(
                        recordingDurationMs = System.currentTimeMillis() - recordStartTimeMs
                    )
                }
            }
        } else {
            // Stop recording
            recordingTimerJob?.cancel()
            recordingTimerJob = null
            val duration = System.currentTimeMillis() - recordStartTimeMs

            if (recordedEvents.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordingDurationMs = 0L,
                    showSaveDialog = false
                )
            } else {
                val packName = _uiState.value.pack?.name ?: "Beat"
                val defaultName = "$packName Track ${(System.currentTimeMillis() % 1000)}"
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    recordingDurationMs = duration,
                    showSaveDialog = true,
                    defaultTrackName = defaultName
                )
            }
        }
    }

    fun saveTrack(trackName: String) {
        val pack = _uiState.value.pack ?: return
        val eventsToMix = recordedEvents.toList()
        val duration = _uiState.value.recordingDurationMs

        _uiState.value = _uiState.value.copy(
            showSaveDialog = false,
            isRendering = true,
            renderProgress = 0.05f
        )

        viewModelScope.launch {
            val context = getApplication<Application>()
            val recDir = File(context.filesDir, "recordings")
            recDir.mkdirs()
            val wavFile = File(recDir, "track_${UUID.randomUUID()}.wav")

            val renderResult = AudioRenderer.renderMixToWav(
                context = context,
                pack = pack,
                events = eventsToMix,
                destinationFile = wavFile,
                recordingDurationMs = duration,
                onProgress = { p ->
                    _uiState.value = _uiState.value.copy(renderProgress = p)
                }
            )

            renderResult.onSuccess { file ->
                val track = TrackEntity(
                    name = trackName.ifBlank { _uiState.value.defaultTrackName },
                    packId = pack.id,
                    createdAt = System.currentTimeMillis(),
                    durationMs = duration.coerceAtLeast(1000L),
                    filePath = file.absolutePath,
                    eventsJson = Json.encodeToString(eventsToMix)
                )
                trackRepository.insertTrack(track)
            }

            recordedEvents.clear()
            _uiState.value = _uiState.value.copy(
                isRendering = false,
                renderProgress = 0.0f,
                recordingDurationMs = 0L,
                recordedEventCount = 0
            )
        }
    }

    fun discardRecording() {
        recordedEvents.clear()
        _uiState.value = _uiState.value.copy(
            showSaveDialog = false,
            isRecording = false,
            recordingDurationMs = 0L,
            recordedEventCount = 0
        )
    }

    fun toggleSide() {
        audioEngine.toggleSide()
    }

    fun setBpm(newBpm: Int) {
        audioEngine.setBpm(newBpm)
    }

    fun toggleMetronome() {
        audioEngine.toggleMetronome()
    }

    fun setQuantizeEnabled(enabled: Boolean) {
        audioEngine.setQuantizeEnabled(enabled)
    }

    fun stopAll() {
        audioEngine.stopAll()
    }

    fun pauseAudio() {
        audioEngine.pauseAll()
    }

    fun resumeAudio() {
        audioEngine.resumeAll()
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        audioEngine.release()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PadViewModel(application) as T
        }
    }
}
