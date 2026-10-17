package com.beatpad.app.ui.tracks

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beatpad.app.data.local.BeatPadDatabase
import com.beatpad.app.data.local.TrackEntity
import com.beatpad.app.data.repository.RoomTrackRepository
import com.beatpad.app.data.repository.TrackRepository
import com.beatpad.app.util.WaveformUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class TracksViewModel(
    application: Application,
    private val trackRepository: TrackRepository = RoomTrackRepository(BeatPadDatabase.getDatabase(application).trackDao())
) : AndroidViewModel(application) {

    val tracks: StateFlow<List<TrackEntity>> = trackRepository.allTracks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _playingTrackId = MutableStateFlow<Long?>(null)
    val playingTrackId: StateFlow<Long?> = _playingTrackId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0.0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val waveformCache = ConcurrentHashMap<Long, List<Float>>()
    private val _waveformsState = MutableStateFlow<Map<Long, List<Float>>>(emptyMap())
    val waveformsState: StateFlow<Map<Long, List<Float>>> = _waveformsState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    fun loadWaveform(track: TrackEntity) {
        if (waveformCache.containsKey(track.id)) return

        viewModelScope.launch {
            val bars = withContext(Dispatchers.IO) {
                WaveformUtil.downsampleFromWavFile(File(track.filePath), 40)
            }
            waveformCache[track.id] = bars
            _waveformsState.value = waveformCache.toMap()
        }
    }

    fun togglePlay(track: TrackEntity) {
        if (_playingTrackId.value == track.id) {
            stopPlayback()
        } else {
            startPlayback(track)
        }
    }

    private fun startPlayback(track: TrackEntity) {
        stopPlayback()

        val file = File(track.filePath)
        if (!file.exists()) {
            return
        }

        runCatching {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopPlayback()
                }
            }
            mediaPlayer = player
            _playingTrackId.value = track.id
            _playbackProgress.value = 0.0f

            progressJob = viewModelScope.launch {
                val totalDuration = player.duration.toFloat().coerceAtLeast(1.0f)
                while (isActive && player.isPlaying) {
                    val current = player.currentPosition.toFloat()
                    _playbackProgress.value = (current / totalDuration).coerceIn(0.0f, 1.0f)
                    delay(50L)
                }
            }
        }.onFailure {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        progressJob?.cancel()
        progressJob = null
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            reset()
            release()
        }
        mediaPlayer = null
        _playingTrackId.value = null
        _playbackProgress.value = 0.0f
    }

    fun renameTrack(id: Long, newName: String) {
        viewModelScope.launch {
            trackRepository.renameTrack(id, newName.trim())
        }
    }

    fun deleteTrack(track: TrackEntity) {
        if (_playingTrackId.value == track.id) {
            stopPlayback()
        }
        waveformCache.remove(track.id)
        _waveformsState.value = waveformCache.toMap()

        viewModelScope.launch {
            trackRepository.deleteTrack(track)
        }
    }

    fun shareTrack(context: Context, track: TrackEntity) {
        val file = File(track.filePath)
        if (!file.exists()) return

        runCatching {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, track.name)
                putExtra(Intent.EXTRA_TEXT, "Listen to my beat '${track.name}' recorded with BeatPad!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Track").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TracksViewModel(application) as T
        }
    }
}
