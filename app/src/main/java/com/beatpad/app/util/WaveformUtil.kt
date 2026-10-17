package com.beatpad.app.util

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object WaveformUtil {

    /**
     * Downsamples 16-bit PCM samples into [barCount] normalized amplitude values in range [0.05f..1.0f].
     */
    fun downsamplePcm(samples: ShortArray, barCount: Int = 40): List<Float> {
        if (samples.isEmpty() || barCount <= 0) {
            return List(barCount.coerceAtLeast(1)) { 0.1f }
        }

        val bars = FloatArray(barCount)
        val chunkSize = samples.size.toDouble() / barCount
        var globalMax = 0.0f

        for (i in 0 until barCount) {
            val start = (i * chunkSize).toInt().coerceIn(0, samples.size - 1)
            val end = ((i + 1) * chunkSize).toInt().coerceIn(start + 1, samples.size)

            var sumSquares = 0.0
            var count = 0
            for (j in start until end) {
                val norm = samples[j] / 32768.0
                sumSquares += norm * norm
                count++
            }

            val rms = if (count > 0) sqrt(sumSquares / count).toFloat() else 0.0f
            bars[i] = rms
            if (rms > globalMax) globalMax = rms
        }

        // Normalize with a minimum floor for clean display
        val scale = if (globalMax > 0.001f) 0.95f / globalMax else 1.0f
        return bars.map { (it * scale).coerceIn(0.08f, 1.0f) }
    }

    /**
     * Reads 16-bit stereo/mono PCM from a standard WAV file and downsamples to [barCount] bars.
     */
    fun downsampleFromWavFile(file: File, barCount: Int = 40): List<Float> {
        if (!file.exists() || file.length() <= 44) {
            return List(barCount) { 0.1f }
        }

        return runCatching {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                fis.read(header)

                val dataSize = (file.length() - 44).toInt()
                val byteBuffer = ByteArray(dataSize)
                var bytesRead = 0
                while (bytesRead < dataSize) {
                    val read = fis.read(byteBuffer, bytesRead, dataSize - bytesRead)
                    if (read == -1) break
                    bytesRead += read
                }

                val shortBuffer = ByteBuffer.wrap(byteBuffer, 0, bytesRead)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer()

                val samples = ShortArray(shortBuffer.remaining())
                shortBuffer.get(samples)
                downsamplePcm(samples, barCount)
            }
        }.getOrDefault(List(barCount) { 0.1f })
    }
}
