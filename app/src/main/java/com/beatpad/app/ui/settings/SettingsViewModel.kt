package com.beatpad.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beatpad.app.audio.AudioEngine
import com.beatpad.app.data.repository.BackupRestoreManager
import com.beatpad.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val hapticsEnabled: Boolean = true,
    val quantizeDefault: Boolean = true,
    val latencyOffsetMs: Int = 0,
    val language: String = "SYSTEM",
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val statusMessage: String? = null
)

class SettingsViewModel(
    application: Application,
    private val preferencesRepository: UserPreferencesRepository = UserPreferencesRepository(application),
    private val backupRestoreManager: BackupRestoreManager = BackupRestoreManager(application),
    private val audioEngine: AudioEngine? = null
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val haptics = preferencesRepository.hapticsEnabled.first()
            val quantize = preferencesRepository.quantizeDefault.first()
            val latency = preferencesRepository.latencyOffsetMs.first()
            val lang = preferencesRepository.appLanguage.first()
            _uiState.value = _uiState.value.copy(
                hapticsEnabled = haptics,
                quantizeDefault = quantize,
                latencyOffsetMs = latency,
                language = lang
            )
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(hapticsEnabled = enabled)
        viewModelScope.launch { preferencesRepository.setHapticsEnabled(enabled) }
    }

    fun setQuantizeDefault(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(quantizeDefault = enabled)
        viewModelScope.launch { preferencesRepository.setQuantizeDefault(enabled) }
    }

    fun setLatencyOffsetMs(offsetMs: Int) {
        val clamped = offsetMs.coerceIn(-100, 100)
        _uiState.value = _uiState.value.copy(latencyOffsetMs = clamped)
        viewModelScope.launch { preferencesRepository.setLatencyOffsetMs(clamped) }
    }

    fun setLanguage(langCode: String) {
        _uiState.value = _uiState.value.copy(language = langCode)
        viewModelScope.launch { preferencesRepository.setAppLanguage(langCode) }
    }

    fun testAudioClick() {
        audioEngine?.onPadDown(0)
    }

    fun exportBackup(uri: Uri) {
        val context = getApplication<Application>()
        _uiState.value = _uiState.value.copy(isBackingUp = true)
        viewModelScope.launch {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    backupRestoreManager.exportBackup(stream).getOrThrow()
                } ?: 0
            }
            _uiState.value = _uiState.value.copy(
                isBackingUp = false,
                statusMessage = if (result.isSuccess) {
                    "Backup exported successfully (${result.getOrDefault(0)} items)"
                } else {
                    "Failed to export backup: ${result.exceptionOrNull()?.message}"
                }
            )
        }
    }

    fun importBackup(uri: Uri) {
        val context = getApplication<Application>()
        _uiState.value = _uiState.value.copy(isRestoring = true)
        viewModelScope.launch {
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    backupRestoreManager.importBackup(stream).getOrThrow()
                } ?: 0
            }
            _uiState.value = _uiState.value.copy(
                isRestoring = false,
                statusMessage = if (result.isSuccess) {
                    "Restored ${result.getOrDefault(0)} items from backup!"
                } else {
                    "Failed to restore backup: ${result.exceptionOrNull()?.message}"
                }
            )
        }
    }

    fun dismissStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    class Factory(
        private val application: Application,
        private val audioEngine: AudioEngine? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application, audioEngine = audioEngine) as T
        }
    }
}
