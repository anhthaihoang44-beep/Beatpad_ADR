package com.beatpad.app

import com.beatpad.app.audio.AudioRenderer
import com.beatpad.app.data.model.RecordingEvent
import com.beatpad.app.util.WaveformUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.math.sin

class RecorderAndTracksTest {

    @Test
    fun testTimelineMixLengthCalculation() {
        val sampleDurations = mapOf(
            "0_SIDE_A" to 500L,
            "1_SIDE_A" to 800L,
            "4_SIDE_A" to 1500L
        )

        // Empty events should fallback to max(recordingDuration, 1000)
        val emptyLen = AudioRenderer.calculateMixDurationMs(emptyList(), sampleDurations, recordingDurationMs = 2000L)
        assertEquals(2000L, emptyLen)

        // Multiple events: event at 1000ms with duration 800ms -> ends at 1800ms + 500ms tail = 2300ms
        val events = listOf(
            RecordingEvent(padIndex = 0, pressTimeMs = 0L, side = "SIDE_A"),
            RecordingEvent(padIndex = 1, pressTimeMs = 1000L, side = "SIDE_A")
        )
        val mixLen = AudioRenderer.calculateMixDurationMs(events, sampleDurations, recordingDurationMs = 1500L, tailMs = 500L)
        assertEquals(2300L, mixLen)

        // Event with loop held until 3000ms -> ends at 3000ms + 200ms + 500ms tail = 3700ms
        val loopEvent = listOf(
            RecordingEvent(padIndex = 4, pressTimeMs = 500L, releaseTimeMs = 3000L, side = "SIDE_A")
        )
        val loopMixLen = AudioRenderer.calculateMixDurationMs(loopEvent, sampleDurations, recordingDurationMs = 3200L, tailMs = 500L)
        assertEquals(3700L, loopMixLen)
    }

    @Test
    fun testWaveformDownsampling() {
        // Create 44100 synthetic PCM samples (1 second) of a sine pulse
        val pcm = ShortArray(44100) { i ->
            val t = i.toDouble() / 44100.0
            (sin(2.0 * Math.PI * 440.0 * t) * 20000.0).toInt().toShort()
        }

        val bars = WaveformUtil.downsamplePcm(pcm, barCount = 40)
        assertEquals(40, bars.size)

        for (bar in bars) {
            assertTrue("Bar value $bar should be >= 0.05", bar >= 0.05f)
            assertTrue("Bar value $bar should be <= 1.0", bar <= 1.0f)
        }

        // Test empty / zero handling
        val emptyBars = WaveformUtil.downsamplePcm(ShortArray(0), barCount = 30)
        assertEquals(30, emptyBars.size)
        assertTrue(emptyBars.all { it >= 0.05f })
    }

    @Test
    fun testWavWriteAndDecodeRoundTrip() {
        val tempFile = File.createTempFile("test_mix_", ".wav")
        try {
            val frameCount = 4410 // 0.1 second at 44.1 kHz
            val left = FloatArray(frameCount) { i -> sin(i * 0.1f) * 0.8f }
            val right = FloatArray(frameCount) { i -> sin(i * 0.1f + 0.5f) * 0.8f }

            AudioRenderer.writeWavFile(tempFile, left, right, frameCount)
            assertTrue(tempFile.exists())
            assertEquals(44L + frameCount * 4L, tempFile.length())

            FileInputStream(tempFile).use { input ->
                val decoded = AudioRenderer.decodeWavStream(input)
                assertEquals(frameCount, decoded.frameCount)
                assertEquals(100L, decoded.durationMs)
                assertEquals(frameCount * 2, decoded.frames.size)

                // Verify peak amplitude preservation
                val maxDecoded = decoded.frames.maxOf { kotlin.math.abs(it) }
                assertTrue("Decoded peak should be close to 0.8, was $maxDecoded", maxDecoded in 0.75f..0.85f)
            }
        } finally {
            tempFile.delete()
        }
    }
}
