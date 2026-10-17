package com.beatpad.app.ui.library

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beatpad.app.data.model.SoundPack
import com.beatpad.app.data.repository.AssetPackRepository
import com.beatpad.app.data.repository.PackRepository
import com.beatpad.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val packs: List<SoundPack> = emptyList(),
    val isLoading: Boolean = true,
    val previewingPackId: String? = null,
    val selectedFilter: String? = null
)

class LibraryViewModel(
    application: Application,
    private val packRepository: PackRepository = AssetPackRepository(application),
    private val preferencesRepository: UserPreferencesRepository = UserPreferencesRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var previewTimerJob: Job? = null

    init {
        loadPacks()
    }

    fun loadPacks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val allPacks = packRepository.getAllPacks()
            _uiState.value = _uiState.value.copy(
                packs = allPacks,
                isLoading = false
            )
        }
    }

    fun togglePreview(pack: SoundPack) {
        if (_uiState.value.previewingPackId == pack.id) {
            stopPreview()
        } else {
            startPreview(pack)
        }
    }

    private fun startPreview(pack: SoundPack) {
        stopPreview()
        val samplePath = pack.demoSample.ifEmpty {
            pack.pads.firstOrNull()?.fileA ?: return
        }
        android.util.Log.d("BeatPadPreview", "Starting preview for pack ${pack.id} at path: $samplePath")

        runCatching {
            val context = getApplication<Application>()
            val player = MediaPlayer()
            try {
                val fd = context.assets.openFd(samplePath)
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
            } catch (e: Exception) {
                android.util.Log.w("BeatPadPreview", "openFd failed, falling back to temp file: ${e.message}")
                val tempFile = java.io.File(context.cacheDir, "preview_temp.wav")
                context.assets.open(samplePath).use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                player.setDataSource(tempFile.absolutePath)
            }
            player.prepare()
            player.isLooping = true
            player.start()
            mediaPlayer = player
            _uiState.value = _uiState.value.copy(previewingPackId = pack.id)
            android.util.Log.d("BeatPadPreview", "Preview playing successfully for ${pack.id}")

            // Auto-stop preview after 8 seconds as specified in Phase 3
            previewTimerJob = viewModelScope.launch {
                delay(8000L)
                stopPreview()
            }
        }.onFailure { err ->
            android.util.Log.e("BeatPadPreview", "Error starting preview for ${pack.id}: ${err.message}", err)
            stopPreview()
        }
    }

    fun stopPreview() {
        previewTimerJob?.cancel()
        previewTimerJob = null
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            reset()
            release()
        }
        mediaPlayer = null
        if (_uiState.value.previewingPackId != null) {
            _uiState.value = _uiState.value.copy(previewingPackId = null)
        }
    }

    fun onPackSelected(packId: String) {
        stopPreview()
        viewModelScope.launch {
            preferencesRepository.setLastPackId(packId)
        }
    }

    fun setFilter(genre: String?) {
        _uiState.value = _uiState.value.copy(selectedFilter = genre)
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(application) as T
        }
    }
}
