package com.beatpad.app.audio

import android.content.Context
import com.beatpad.app.data.model.PadMode
import com.beatpad.app.data.model.RecordingEvent
import com.beatpad.app.data.model.SoundPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

object AudioRenderer {

    const val SAMPLE_RATE = 44100
    const val CHANNELS = 2
    const val BITS_PER_SAMPLE = 16

    data class DecodedSample(
        val frames: FloatArray, // Interleaved L, R normalized to -1.0f..1.0f
        val frameCount: Int,
        val durationMs: Long
    )

    fun calculateMixDurationMs(
        events: List<RecordingEvent>,
        sampleDurationsMs: Map<String, Long>,
        recordingDurationMs: Long = 0L,
        tailMs: Long = 500L
    ): Long {
        if (events.isEmpty()) return max(recordingDurationMs, 1000L)

        var maxEndMs = recordingDurationMs
        for (event in events) {
            val key = "${event.padIndex}_${event.side}"
            val sampleDuration = sampleDurationsMs[key] ?: 1000L
            val eventEnd = if (event.releaseTimeMs > event.pressTimeMs) {
                max(event.pressTimeMs + sampleDuration, event.releaseTimeMs + 200L)
            } else {
                event.pressTimeMs + sampleDuration
            }
            if (eventEnd > maxEndMs) {
                maxEndMs = eventEnd
            }
        }
        return maxEndMs + tailMs
    }

    suspend fun renderMixToWav(
        context: Context,
        pack: SoundPack,
        events: List<RecordingEvent>,
        destinationFile: File,
        recordingDurationMs: Long = 0L,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.Default) {
        runCatching {
            onProgress(0.05f)

            // 1. Decode needed samples from assets
            val sampleCache = mutableMapOf<String, DecodedSample>()
            val padMap = pack.pads.associateBy { it.index }

            val neededKeys = events.map { "${it.padIndex}_${it.side}" }.toSet()
            val totalKeys = max(1, neededKeys.size)
            var loadedKeys = 0

            for (key in neededKeys) {
                val parts = key.split("_")
                val padIndex = parts[0].toIntOrNull() ?: continue
                val side = parts.getOrNull(1) ?: "SIDE_A"
                val pad = padMap[padIndex] ?: continue
                val assetPath = if (side == "SIDE_B" && pad.fileB.isNotEmpty()) pad.fileB else pad.fileA

                runCatching {
                    val file = File(assetPath)
                    val stream = if (file.exists()) file.inputStream() else context.assets.open(assetPath)
                    stream.use { input ->
                        val decoded = decodeWavStream(input)
                        sampleCache[key] = decoded
                    }
                }
                loadedKeys++
                onProgress(0.05f + (loadedKeys.toFloat() / totalKeys) * 0.25f)
            }

            // 2. Determine mix duration and buffer length
            val sampleDurations = sampleCache.mapValues { it.value.durationMs }
            val mixDurationMs = calculateMixDurationMs(events, sampleDurations, recordingDurationMs)
            val totalFrames = ((mixDurationMs * SAMPLE_RATE) / 1000L).toInt().coerceAtLeast(SAMPLE_RATE)
            val masterL = FloatArray(totalFrames)
            val masterR = FloatArray(totalFrames)

            onProgress(0.35f)

            // 3. Mix events into master buffer
            val totalEvents = max(1, events.size)
            for ((idx, event) in events.withIndex()) {
                val key = "${event.padIndex}_${event.side}"
                val sample = sampleCache[key] ?: continue
                val startFrame = ((event.pressTimeMs * SAMPLE_RATE) / 1000L).toInt().coerceIn(0, totalFrames - 1)
                val pad = padMap[event.padIndex]

                val isLoop = pad?.mode == PadMode.LOOP
                val releaseFrame = if (isLoop && event.releaseTimeMs > event.pressTimeMs) {
                    ((event.releaseTimeMs * SAMPLE_RATE) / 1000L).toInt().coerceIn(0, totalFrames)
                } else {
                    totalFrames
                }

                var currentFrame = startFrame
                var sampleFrameIndex = 0

                while (currentFrame < totalFrames && currentFrame < releaseFrame) {
                    val sampleLeft = sample.frames[sampleFrameIndex * 2]
                    val sampleRight = sample.frames[sampleFrameIndex * 2 + 1]

                    masterL[currentFrame] += sampleLeft
                    masterR[currentFrame] += sampleRight

                    sampleFrameIndex++
                    if (sampleFrameIndex >= sample.frameCount) {
                        if (isLoop && currentFrame < releaseFrame) {
                            sampleFrameIndex = 0 // Loop repeat
                        } else {
                            break
                        }
                    }
                    currentFrame++
                }

                onProgress(0.35f + ((idx + 1).toFloat() / totalEvents) * 0.40f)
            }

            // 4. Clipping protection (Peak normalization + soft limiter)
            onProgress(0.80f)
            var maxPeak = 0.0f
            for (i in 0 until totalFrames) {
                val peakL = abs(masterL[i])
                val peakR = abs(masterR[i])
                if (peakL > maxPeak) maxPeak = peakL
                if (peakR > maxPeak) maxPeak = peakR
            }

            val scale = if (maxPeak > 0.95f) 0.95f / maxPeak else 1.0f
            for (i in 0 until totalFrames) {
                masterL[i] = (masterL[i] * scale).coerceIn(-1.0f, 1.0f)
                masterR[i] = (masterR[i] * scale).coerceIn(-1.0f, 1.0f)
            }

            // 5. Write to WAV file
            onProgress(0.90f)
            destinationFile.parentFile?.mkdirs()
            writeWavFile(destinationFile, masterL, masterR, totalFrames)

            onProgress(1.0f)
            destinationFile
        }
    }

    fun decodeWavStream(input: InputStream): DecodedSample {
        val bytes = input.readBytes()
        if (bytes.size < 44) {
            return DecodedSample(FloatArray(0), 0, 0L)
        }

        val channels = ByteBuffer.wrap(bytes, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
        val sampleRate = ByteBuffer.wrap(bytes, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int

        // Find "data" subchunk
        var dataOffset = 12
        var dataSize = bytes.size - 44
        while (dataOffset + 8 <= bytes.size) {
            val chunkId = String(bytes, dataOffset, 4)
            val chunkSize = ByteBuffer.wrap(bytes, dataOffset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (chunkId == "data") {
                dataOffset += 8
                dataSize = chunkSize.coerceAtMost(bytes.size - dataOffset)
                break
            }
            dataOffset += 8 + chunkSize
        }
        if (dataOffset >= bytes.size) dataOffset = 44

        val shortCount = dataSize / 2
        val shortBuffer = ByteBuffer.wrap(bytes, dataOffset, dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        val rawShorts = ShortArray(shortCount)
        shortBuffer.get(rawShorts)

        val frameCount = if (channels == 2) shortCount / 2 else shortCount
        val interleavedFloats = FloatArray(frameCount * 2)

        if (channels == 2) {
            for (i in 0 until frameCount) {
                interleavedFloats[i * 2] = rawShorts[i * 2] / 32768.0f
                interleavedFloats[i * 2 + 1] = rawShorts[i * 2 + 1] / 32768.0f
            }
        } else {
            // Mono -> Duplicate to Stereo
            for (i in 0 until frameCount) {
                val sample = rawShorts[i] / 32768.0f
                interleavedFloats[i * 2] = sample
                interleavedFloats[i * 2 + 1] = sample
            }
        }

        val durationMs = if (sampleRate > 0) (frameCount * 1000L) / sampleRate else 0L
        return DecodedSample(interleavedFloats, frameCount, durationMs)
    }

    fun writeWavFile(destination: File, masterL: FloatArray, masterR: FloatArray, frameCount: Int) {
        val totalAudioLen = frameCount * 4L
        val totalDataLen = totalAudioLen + 36L
        val byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)

        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buf.put("RIFF".toByteArray())
        buf.putInt(totalDataLen.toInt())
        buf.put("WAVE".toByteArray())

        // fmt subchunk
        buf.put("fmt ".toByteArray())
        buf.putInt(16) // Subchunk1Size (16 for PCM)
        buf.putShort(1) // AudioFormat (1 = PCM)
        buf.putShort(CHANNELS.toShort())
        buf.putInt(SAMPLE_RATE)
        buf.putInt(byteRate)
        buf.putShort((CHANNELS * BITS_PER_SAMPLE / 8).toShort()) // BlockAlign
        buf.putShort(BITS_PER_SAMPLE.toShort())

        // data subchunk
        buf.put("data".toByteArray())
        buf.putInt(totalAudioLen.toInt())

        FileOutputStream(destination).use { fos ->
            fos.write(header)
            val chunkFrames = 2048
            val pcmBytes = ByteArray(chunkFrames * 4)
            var framesWritten = 0

            while (framesWritten < frameCount) {
                val framesToWrite = (frameCount - framesWritten).coerceAtMost(chunkFrames)
                val pcmBuf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
                pcmBuf.clear()

                for (f in 0 until framesToWrite) {
                    val frameIndex = framesWritten + f
                    val shortL = (masterL[frameIndex] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                    val shortR = (masterR[frameIndex] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                    pcmBuf.putShort(shortL)
                    pcmBuf.putShort(shortR)
                }

                fos.write(pcmBytes, 0, framesToWrite * 4)
                framesWritten += framesToWrite
            }
        }
    }
}
