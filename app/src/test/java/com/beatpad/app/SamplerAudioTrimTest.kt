package com.beatpad.app

import com.beatpad.app.util.AudioRecordSampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class SamplerAudioTrimTest {

    @Test
    fun testTrimSilence() {
        val input = floatArrayOf(
            0.001f, 0.005f, 0.01f, // Silence
            0.5f, 0.8f, -0.6f,     // Sound
            0.002f, 0.0f           // Silence
        )
        val trimmed = AudioRecordSampler.trimSilence(input, threshold = 0.02f)
        assertEquals(3, trimmed.size)
        assertEquals(0.5f, trimmed[0], 0.0001f)
        assertEquals(0.8f, trimmed[1], 0.0001f)
        assertEquals(-0.6f, trimmed[2], 0.0001f)
    }

    @Test
    fun testNormalizeAudio() {
        val quietAudio = floatArrayOf(0.1f, -0.2f, 0.15f, -0.05f)
        val normalized = AudioRecordSampler.normalizeAudio(quietAudio)
        var maxPeak = 0f
        for (s in normalized) {
            val a = abs(s)
            if (a > maxPeak) maxPeak = a
        }
        // Peak should be scaled up to 0.95f
        assertEquals(0.95f, maxPeak, 0.001f)
    }

    @Test
    fun testSliceAudio() {
        val samples = FloatArray(1000) { it.toFloat() }
        val sliced = AudioRecordSampler.sliceAudio(samples, 0.2f, 0.6f)
        assertEquals(400, sliced.size)
        assertEquals(200f, sliced[0], 0.0001f)
        assertEquals(599f, sliced[sliced.size - 1], 0.0001f)
    }

    @Test
    fun testWavHeaderSerialization() {
        val tempFile = File.createTempFile("test_sample", ".wav")
        try {
            val samples = floatArrayOf(0.0f, 0.5f, -0.5f, 0.25f, -0.25f)
            AudioRecordSampler.saveToWavFile(samples, tempFile)

            assertTrue(tempFile.exists())
            // 44 bytes header + (5 samples * 2 bytes = 10 bytes) = 54 bytes
            assertEquals(54L, tempFile.length())

            val bytes = tempFile.readBytes()
            val riff = String(bytes, 0, 4)
            val wave = String(bytes, 8, 4)
            val fmt = String(bytes, 12, 4)
            val data = String(bytes, 36, 4)

            assertEquals("RIFF", riff)
            assertEquals("WAVE", wave)
            assertEquals("fmt ", fmt)
            assertEquals("data", data)
        } finally {
            tempFile.delete()
        }
    }
}
