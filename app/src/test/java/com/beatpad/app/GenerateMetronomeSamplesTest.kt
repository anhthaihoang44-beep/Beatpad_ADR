package com.beatpad.app

import com.beatpad.app.util.WavUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.sin

class GenerateMetronomeSamplesTest {

    @Test
    fun generateMetronomeClicks() {
        val candidates = listOf(
            File("src/main/assets/samples"),
            File("app/src/main/assets/samples")
        )
        val assetsDir = candidates.find { it.parentFile?.exists() == true } ?: candidates.first()
        if (!assetsDir.exists()) {
            assetsDir.mkdirs()
        }

        val highFile = File(assetsDir, "metronome_high.wav")
        val lowFile = File(assetsDir, "metronome_low.wav")

        generateClick(
            file = highFile,
            frequency = 1500.0,
            durationSeconds = 0.035,
            sampleRate = 44100
        )

        generateClick(
            file = lowFile,
            frequency = 900.0,
            durationSeconds = 0.035,
            sampleRate = 44100
        )

        assertTrue(highFile.exists() && highFile.length() > 44)
        assertTrue(lowFile.exists() && lowFile.length() > 44)
    }

    private fun generateClick(
        file: File,
        frequency: Double,
        durationSeconds: Double,
        sampleRate: Int
    ) {
        val numSamples = (durationSeconds * sampleRate).toInt()
        val pcmData = ByteArray(numSamples * 2)
        val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSeconds
            val envelope = exp(-20.0 * progress)
            val sampleVal = sin(2.0 * Math.PI * frequency * t) * envelope * 0.9
            val shortVal = (sampleVal * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(shortVal)
        }

        FileOutputStream(file).use { fos ->
            WavUtils.writeWavHeader(
                out = fos,
                audioLength = pcmData.size,
                sampleRate = sampleRate,
                channels = 1,
                bitsPerSample = 16
            )
            fos.write(pcmData)
        }
    }
}
