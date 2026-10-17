package com.beatpad.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import com.beatpad.app.data.model.PackSide
import com.beatpad.app.data.model.PadMode
import com.beatpad.app.data.model.SoundPack
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

class SoundPoolAudioEngine(
    private val context: Context
) : AudioEngine, AudioManager.OnAudioFocusChangeListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(32)
        .setAudioAttributes(audioAttributes)
        .build()

    private val _isPreloaded = MutableStateFlow(false)
    override val isPreloaded: StateFlow<Boolean> = _isPreloaded.asStateFlow()

    private val _activeLoops = MutableStateFlow<Set<Int>>(emptySet())
    override val activeLoops: StateFlow<Set<Int>> = _activeLoops.asStateFlow()

    private val _pendingLoops = MutableStateFlow<Set<Int>>(emptySet())
    override val pendingLoops: StateFlow<Set<Int>> = _pendingLoops.asStateFlow()

    private val _currentBpm = MutableStateFlow(140)
    override val currentBpm: StateFlow<Int> = _currentBpm.asStateFlow()

    private val _isMetronomeActive = MutableStateFlow(false)
    override val isMetronomeActive: StateFlow<Boolean> = _isMetronomeActive.asStateFlow()

    private val _isQuantizeEnabled = MutableStateFlow(true)
    override val isQuantizeEnabled: StateFlow<Boolean> = _isQuantizeEnabled.asStateFlow()

    private val _currentSide = MutableStateFlow(PackSide.SIDE_A)
    override val currentSide: StateFlow<PackSide> = _currentSide.asStateFlow()

    private val sampleIdsA = ConcurrentHashMap<Int, Int>()
    private val sampleIdsB = ConcurrentHashMap<Int, Int>()
    private val activeStreams = ConcurrentHashMap<Int, Int>()
    private var metroHighId: Int = 0
    private var metroLowId: Int = 0
    private var currentPack: SoundPack? = null

    private val masterClock = MasterClock(
        bpm = 140,
        beatsPerBar = 4,
        onBeat = { beatIndex ->
            if (_isMetronomeActive.value) {
                val soundId = if (beatIndex == 0) metroHighId else metroLowId
                if (soundId != 0) soundPool.play(soundId, 0.7f, 0.7f, 2, 0, 1.0f)
            }
        },
        onBarBoundary = { triggerPendingLoopsOnBarBoundary() }
    )

    override suspend fun preload(pack: SoundPack): Boolean = withContext(Dispatchers.IO) {
        _isPreloaded.value = false
        stopAll()
        sampleIdsA.clear()
        sampleIdsB.clear()
        currentPack = pack
        _currentBpm.value = pack.bpm
        masterClock.setBpm(pack.bpm)

        var expectedCount = 0
        var loadedCount = 0
        val loadDeferred = CompletableDeferred<Boolean>()

        soundPool.setOnLoadCompleteListener { _, _, _ ->
            loadedCount++
            if (loadedCount >= expectedCount && !loadDeferred.isCompleted) {
                _isPreloaded.value = true
                loadDeferred.complete(true)
            }
        }

        // Metronome samples
        runCatching {
            context.assets.openFd("samples/metronome_high.wav").use { fd ->
                metroHighId = soundPool.load(fd, 2)
                expectedCount++
            }
        }
        runCatching {
            context.assets.openFd("samples/metronome_low.wav").use { fd ->
                metroLowId = soundPool.load(fd, 2)
                expectedCount++
            }
        }

        // Pack samples for Side A and Side B
        fun loadSample(path: String): Int? {
            if (path.isBlank()) return null
            val file = java.io.File(path)
            return if (file.exists()) {
                soundPool.load(file.absolutePath, 1)
            } else {
                runCatching { context.assets.openFd(path).use { soundPool.load(it, 1) } }.getOrNull()
            }
        }
        for (pad in pack.pads) {
            loadSample(pad.fileA)?.let { sampleIdsA[pad.index] = it; expectedCount++ }
            loadSample(pad.fileB)?.let { sampleIdsB[pad.index] = it; expectedCount++ }
        }

        if (expectedCount == 0 || loadedCount >= expectedCount) {
            _isPreloaded.value = true
            loadDeferred.complete(true)
        }

        requestAudioFocus()
        masterClock.start()
        withTimeoutOrNull(4000L) { loadDeferred.await() }
        _isPreloaded.value = true
        true
    }

    override fun onPadDown(index: Int) {
        val pack = currentPack ?: return
        val pad = pack.pads.find { it.index == index } ?: return
        val rate = Quantizer.calculatePlaybackRate(_currentBpm.value, pack.bpm)
        val soundId = getSoundIdForPad(index) ?: return

        if (pad.mode == PadMode.ONE_SHOT) {
            val streamId = soundPool.play(soundId, 1f, 1f, 1, 0, rate)
            if (streamId != 0) activeStreams[index] = streamId
        } else {
            val currentLoops = _activeLoops.value
            val currentPending = _pendingLoops.value
            when {
                currentLoops.contains(index) -> stopPad(index)
                currentPending.contains(index) -> _pendingLoops.value = currentPending - index
                _isQuantizeEnabled.value -> _pendingLoops.value = currentPending + index
                else -> startLoopStream(index, soundId, rate)
            }
        }
    }

    private fun triggerPendingLoopsOnBarBoundary() {
        val pending = _pendingLoops.value
        if (pending.isEmpty()) return
        val pack = currentPack ?: return
        val rate = Quantizer.calculatePlaybackRate(_currentBpm.value, pack.bpm)

        for (padIndex in pending) {
            val soundId = getSoundIdForPad(padIndex)
            if (soundId != null) startLoopStream(padIndex, soundId, rate)
        }
        _pendingLoops.value = emptySet()
    }

    private fun startLoopStream(index: Int, soundId: Int, rate: Float) {
        val streamId = soundPool.play(soundId, 1f, 1f, 1, -1, rate)
        if (streamId != 0) {
            activeStreams[index] = streamId
            _activeLoops.value = _activeLoops.value + index
        }
    }

    private fun getSoundIdForPad(index: Int): Int? {
        val map = if (_currentSide.value == PackSide.SIDE_A) sampleIdsA else sampleIdsB
        val id = map[index]
        return if (id != null && id != 0) id else null
    }

    override fun onPadUp(index: Int) {}

    override fun stopPad(index: Int) {
        val streamId = activeStreams.remove(index)
        if (streamId != null) soundPool.stop(streamId)
        _activeLoops.value = _activeLoops.value - index
        _pendingLoops.value = _pendingLoops.value - index
    }

    override fun setBpm(newBpm: Int) {
        _currentBpm.value = newBpm
        masterClock.setBpm(newBpm)
        val packBpm = currentPack?.bpm ?: newBpm
        val newRate = Quantizer.calculatePlaybackRate(newBpm, packBpm)
        for ((_, streamId) in activeStreams) {
            soundPool.setRate(streamId, newRate)
        }
    }

    override fun toggleMetronome() {
        _isMetronomeActive.value = !_isMetronomeActive.value
    }

    override fun setQuantizeEnabled(enabled: Boolean) {
        _isQuantizeEnabled.value = enabled
    }

    override fun toggleSide(): PackSide {
        val newSide = _currentSide.value.toggle()
        _currentSide.value = newSide

        val activeIndices = _activeLoops.value
        val pack = currentPack
        if (pack != null && activeIndices.isNotEmpty()) {
            val rate = Quantizer.calculatePlaybackRate(_currentBpm.value, pack.bpm)
            for (idx in activeIndices) {
                val oldStream = activeStreams.remove(idx)
                if (oldStream != null) soundPool.stop(oldStream)
                val newSoundId = getSoundIdForPad(idx)
                if (newSoundId != null) {
                    val newStream = soundPool.play(newSoundId, 1f, 1f, 1, -1, rate)
                    if (newStream != 0) activeStreams[idx] = newStream
                }
            }
        }
        return newSide
    }

    override fun stopAll() {
        for ((_, streamId) in activeStreams) soundPool.stop(streamId)
        activeStreams.clear()
        _activeLoops.value = emptySet()
        _pendingLoops.value = emptySet()
    }

    override fun pauseAll() {
        soundPool.autoPause()
        masterClock.stop()
    }

    override fun resumeAll() {
        soundPool.autoResume()
        masterClock.start()
    }

    override fun release() {
        abandonAudioFocus()
        masterClock.stop()
        stopAll()
        soundPool.release()
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(this)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseAll()
            AudioManager.AUDIOFOCUS_GAIN -> resumeAll()
        }
    }
}
