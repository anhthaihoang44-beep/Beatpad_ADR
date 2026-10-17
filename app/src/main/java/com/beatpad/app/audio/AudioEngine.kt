package com.beatpad.app.audio

import com.beatpad.app.data.model.PackSide
import com.beatpad.app.data.model.SoundPack
import kotlinx.coroutines.flow.StateFlow

interface AudioEngine {
    val isPreloaded: StateFlow<Boolean>
    val activeLoops: StateFlow<Set<Int>>
    val pendingLoops: StateFlow<Set<Int>>
    val currentBpm: StateFlow<Int>
    val isMetronomeActive: StateFlow<Boolean>
    val isQuantizeEnabled: StateFlow<Boolean>
    val currentSide: StateFlow<PackSide>

    suspend fun preload(pack: SoundPack): Boolean
    fun onPadDown(index: Int)
    fun onPadUp(index: Int)
    fun stopPad(index: Int)
    fun stopAll()
    fun pauseAll()
    fun resumeAll()
    fun setBpm(newBpm: Int)
    fun toggleMetronome()
    fun setQuantizeEnabled(enabled: Boolean)
    fun toggleSide(): PackSide
    fun release()
}
