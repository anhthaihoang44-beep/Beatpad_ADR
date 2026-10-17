package com.beatpad.app.audio

/**
 * Pure mathematical logic for tempo scaling, quantize boundaries, and bar timing.
 */
object Quantizer {

    const val MIN_PLAYBACK_RATE = 0.5f
    const val MAX_PLAYBACK_RATE = 2.0f
    private const val NANOS_PER_MINUTE = 60_000_000_000L

    /**
     * Calculates SoundPool playback rate based on pack BPM vs current BPM.
     * Clamped between 0.5 and 2.0 as required.
     */
    fun calculatePlaybackRate(currentBpm: Int, packBpm: Int): Float {
        if (packBpm <= 0) return 1.0f
        val rawRate = currentBpm.toFloat() / packBpm.toFloat()
        return rawRate.coerceIn(MIN_PLAYBACK_RATE, MAX_PLAYBACK_RATE)
    }

    /**
     * Calculates duration of one beat (quarter note) in nanoseconds.
     */
    fun calculateBeatDurationNanos(bpm: Int): Long {
        val safeBpm = bpm.coerceAtLeast(1)
        return NANOS_PER_MINUTE / safeBpm
    }

    /**
     * Calculates duration of one bar (default 4 beats for 4/4) in nanoseconds.
     */
    fun calculateBarDurationNanos(bpm: Int, beatsPerBar: Int = 4): Long {
        return calculateBeatDurationNanos(bpm) * beatsPerBar
    }

    /**
     * Computes nanoseconds remaining until the next bar boundary relative to startNanos.
     */
    fun calculateDelayToNextBarNanos(currentNanos: Long, startNanos: Long, barDurationNanos: Long): Long {
        if (barDurationNanos <= 0L) return 0L
        val elapsed = (currentNanos - startNanos).coerceAtLeast(0L)
        val remainder = elapsed % barDurationNanos
        val delay = barDurationNanos - remainder
        return if (delay == barDurationNanos && remainder == 0L) 0L else delay
    }

    /**
     * Computes nanoseconds remaining until the next beat boundary relative to startNanos.
     */
    fun calculateDelayToNextBeatNanos(currentNanos: Long, startNanos: Long, beatDurationNanos: Long): Long {
        if (beatDurationNanos <= 0L) return 0L
        val elapsed = (currentNanos - startNanos).coerceAtLeast(0L)
        val remainder = elapsed % beatDurationNanos
        val delay = beatDurationNanos - remainder
        return if (delay == beatDurationNanos && remainder == 0L) 0L else delay
    }
}
