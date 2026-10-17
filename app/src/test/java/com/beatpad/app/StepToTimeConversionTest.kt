package com.beatpad.app

import com.beatpad.app.audio.StepTimingUtils
import com.beatpad.app.data.model.LoopPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepToTimeConversionTest {

    @Test
    fun testStepDurationCalculation() {
        // 120 BPM: (60,000 / 120) / 4 = 125.0 ms per 16th note step
        assertEquals(125.0, StepTimingUtils.calculateStepDurationMs(120), 0.001)

        // 60 BPM: (60,000 / 60) / 4 = 250.0 ms per step
        assertEquals(250.0, StepTimingUtils.calculateStepDurationMs(60), 0.001)

        // 140 BPM: (60,000 / 140) / 4 = 107.142857 ms
        assertEquals(107.1428, StepTimingUtils.calculateStepDurationMs(140), 0.001)
    }

    @Test
    fun testSwingOffsetCalculation() {
        val stepDuration = 125.0 // 120 BPM

        // Even steps never have swing delay
        assertEquals(0.0, StepTimingUtils.calculateSwingOffsetMs(0, stepDuration, 50), 0.001)
        assertEquals(0.0, StepTimingUtils.calculateSwingOffsetMs(2, stepDuration, 50), 0.001)
        assertEquals(0.0, StepTimingUtils.calculateSwingOffsetMs(4, stepDuration, 50), 0.001)

        // Odd step with 0% swing has 0 offset
        assertEquals(0.0, StepTimingUtils.calculateSwingOffsetMs(1, stepDuration, 0), 0.001)

        // Odd step with 50% swing delays by (50/100) * (125 * 0.5) = 31.25 ms
        assertEquals(31.25, StepTimingUtils.calculateSwingOffsetMs(1, stepDuration, 50), 0.001)

        // Odd step with 20% swing delays by (20/100) * (125 * 0.5) = 12.5 ms
        assertEquals(12.5, StepTimingUtils.calculateSwingOffsetMs(3, stepDuration, 20), 0.001)
    }

    @Test
    fun testCalculateStepTimeMs() {
        // At 120 BPM (125ms per step), 0% swing:
        assertEquals(0L, StepTimingUtils.calculateStepTimeMs(0, 120, 0))
        assertEquals(125L, StepTimingUtils.calculateStepTimeMs(1, 120, 0))
        assertEquals(500L, StepTimingUtils.calculateStepTimeMs(4, 120, 0)) // Beat 2
        assertEquals(1000L, StepTimingUtils.calculateStepTimeMs(8, 120, 0)) // Beat 3
        assertEquals(1500L, StepTimingUtils.calculateStepTimeMs(12, 120, 0)) // Beat 4

        // With 50% swing on step 1: 125 + 31.25 = 156.25 -> 156L
        assertEquals(156L, StepTimingUtils.calculateStepTimeMs(1, 120, 50))
    }

    @Test
    fun testGenerateEventsForFourBars() {
        // Kick on steps 0, 4, 8, 12 (4-on-the-floor)
        var pattern = LoopPattern.empty("test_pack", 120)
        pattern = pattern.toggleStep(0, 0)
        pattern = pattern.toggleStep(0, 4)
        pattern = pattern.toggleStep(0, 8)
        pattern = pattern.toggleStep(0, 12)

        val events = StepTimingUtils.generateEventsForBars(pattern, totalBars = 4)

        // 4 hits per bar * 4 bars = 16 events
        assertEquals(16, events.size)

        // Ensure events are chronologically ordered
        for (i in 0 until events.size - 1) {
            assertTrue(events[i].pressTimeMs <= events[i + 1].pressTimeMs)
        }

        // First event at 0ms, last event around bar 4 beat 4 (6000ms - 500ms = 6000ms at 120bpm)
        assertEquals(0L, events.first().pressTimeMs)
        assertEquals(7500L, events.last().pressTimeMs) // Bar 3 start is 6000ms, step 12 is 1500ms -> 7500ms
    }
}
