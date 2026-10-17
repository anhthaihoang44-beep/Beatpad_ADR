package com.beatpad.app.audio

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MasterClock(
    private var bpm: Int = 140,
    private val beatsPerBar: Int = 4,
    private val onBeat: (beatIndex: Int) -> Unit = {},
    private val onBarBoundary: () -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var clockJob: Job? = null

    @Volatile
    private var isRunning = false

    @Volatile
    var startNanos: Long = 0L
        private set

    fun setBpm(newBpm: Int) {
        bpm = newBpm.coerceAtLeast(1)
    }

    fun getBpm(): Int = bpm

    fun start() {
        if (isRunning) return
        isRunning = true
        startNanos = SystemClock.elapsedRealtimeNanos()

        clockJob = scope.launch {
            var currentBeat = 0
            while (isActive && isRunning) {
                val nowNanos = SystemClock.elapsedRealtimeNanos()
                val beatDurationNanos = Quantizer.calculateBeatDurationNanos(bpm)

                // Trigger beat
                onBeat(currentBeat)

                // If beat 0 of bar, trigger bar boundary
                if (currentBeat == 0) {
                    onBarBoundary()
                }

                currentBeat = (currentBeat + 1) % beatsPerBar

                // Calculate next beat target time to prevent drift
                val nextBeatTargetNanos = nowNanos + beatDurationNanos
                val sleepNanos = nextBeatTargetNanos - SystemClock.elapsedRealtimeNanos()
                val sleepMillis = (sleepNanos / 1_000_000L).coerceAtLeast(1L)
                delay(sleepMillis)
            }
        }
    }

    fun stop() {
        isRunning = false
        clockJob?.cancel()
        clockJob = null
    }
}
