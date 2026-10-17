package com.beatpad.app

import com.beatpad.app.audio.Quantizer
import org.junit.Assert.assertEquals
import org.junit.Test

class QuantizerTest {

    @Test
    fun testPlaybackRate_exactAndClamping() {
        // Normal 1.0 rate
        assertEquals(1.0f, Quantizer.calculatePlaybackRate(140, 140), 0.001f)

        // Faster rate
        assertEquals(1.1428f, Quantizer.calculatePlaybackRate(160, 140), 0.001f)

        // Slower rate
        assertEquals(0.8571f, Quantizer.calculatePlaybackRate(120, 140), 0.001f)

        // Clamped to minimum 0.5f
        assertEquals(0.5f, Quantizer.calculatePlaybackRate(40, 140), 0.001f)

        // Clamped to maximum 2.0f
        assertEquals(2.0f, Quantizer.calculatePlaybackRate(320, 140), 0.001f)
    }

    @Test
    fun testBeatAndBarDurationCalculations() {
        // At 120 BPM, 1 beat = 0.5 seconds = 500_000_000 ns
        val beatDuration120 = Quantizer.calculateBeatDurationNanos(120)
        assertEquals(500_000_000L, beatDuration120)

        // 4 beats per bar = 2.0 seconds = 2_000_000_000 ns
        val barDuration120 = Quantizer.calculateBarDurationNanos(120, 4)
        assertEquals(2_000_000_000L, barDuration120)

        // At 60 BPM, 1 beat = 1.0 second = 1_000_000_000 ns
        val beatDuration60 = Quantizer.calculateBeatDurationNanos(60)
        assertEquals(1_000_000_000L, beatDuration60)
    }

    @Test
    fun testDelayToNextBarBoundary() {
        val barNanos = 2_000_000_000L // 2 seconds
        val startNanos = 10_000_000_000L

        // Exactly on bar boundary (elapsed = 0)
        val delayAtStart = Quantizer.calculateDelayToNextBarNanos(10_000_000_000L, startNanos, barNanos)
        assertEquals(0L, delayAtStart)

        // Halfway through bar (elapsed = 1s, remaining = 1s)
        val delayHalfway = Quantizer.calculateDelayToNextBarNanos(11_000_000_000L, startNanos, barNanos)
        assertEquals(1_000_000_000L, delayHalfway)

        // 1.8s into bar (remaining = 0.2s = 200_000_000 ns)
        val delayNearEnd = Quantizer.calculateDelayToNextBarNanos(11_800_000_000L, startNanos, barNanos)
        assertEquals(200_000_000L, delayNearEnd)
    }
}
