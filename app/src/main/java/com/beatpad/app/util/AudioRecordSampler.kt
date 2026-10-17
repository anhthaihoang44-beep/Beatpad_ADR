package com.beatpad.app.util

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

object AudioRecordSampler {

    const val SAMPLE_RATE = 44100
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val MAX_RECORD_SECONDS = 3

    @SuppressLint("MissingPermission")
    suspend fun recordFromMic(
        maxDurationMs: Long = MAX_RECORD_SECONDS * 1000L,
        onProgress: (elapsedMs: Long) -> Unit = {}
    ): Result<FloatArray> = withContext(Dispatchers.IO) {
        runCatching {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = max(minBufferSize, SAMPLE_RATE * 2)

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                error("AudioRecord failed to initialize")
            }

            val maxShorts = ((maxDurationMs * SAMPLE_RATE) / 1000L).toInt()
            val recordedShorts = ShortArray(maxShorts)
            val tempBuffer = ShortArray(1024)
            var totalRead = 0

            recorder.startRecording()
            val startTime = System.currentTimeMillis()

            try {
                while (totalRead < maxShorts) {
                    val read = recorder.read(tempBuffer, 0, minOf(tempBuffer.size, maxShorts - totalRead))
                    if (read > 0) {
                        System.arraycopy(tempBuffer, 0, recordedShorts, totalRead, read)
                        totalRead += read
                        val elapsed = System.currentTimeMillis() - startTime
                        onProgress(elapsed.coerceAtMost(maxDurationMs))
                    } else {
                        break
                    }
                }
            } finally {
                runCatching {
                    recorder.stop()
                    recorder.release()
                }
            }

            // Convert to normalized FloatArray (-1.0f .. 1.0f)
            val floats = FloatArray(totalRead)
            for (i in 0 until totalRead) {
                floats[i] = recordedShorts[i] / 32768.0f
            }
            normalizeAudio(trimSilence(floats))
        }
    }

    fun trimSilence(samples: FloatArray, threshold: Float = 0.02f): FloatArray {
        var start = 0
        while (start < samples.size && abs(samples[start]) < threshold) {
            start++
        }
        var end = samples.size - 1
        while (end > start && abs(samples[end]) < threshold) {
            end--
        }
        if (start >= end) return samples
        return samples.copyOfRange(start, end + 1)
    }

    fun normalizeAudio(samples: FloatArray): FloatArray {
        var maxPeak = 0.0f
        for (s in samples) {
            val a = abs(s)
            if (a > maxPeak) maxPeak = a
        }
        if (maxPeak <= 0.001f || maxPeak >= 0.98f) return samples
        val scale = 0.95f / maxPeak
        val normalized = FloatArray(samples.size)
        for (i in samples.indices) {
            normalized[i] = (samples[i] * scale).coerceIn(-1.0f, 1.0f)
        }
        return normalized
    }

    fun sliceAudio(samples: FloatArray, startFraction: Float, endFraction: Float): FloatArray {
        val s = (startFraction.coerceIn(0f, 1f) * samples.size).toInt()
        val e = (endFraction.coerceIn(0f, 1f) * samples.size).toInt().coerceAtLeast(s + 100)
        return samples.copyOfRange(s, minOf(e, samples.size))
    }

    fun saveToWavFile(samples: FloatArray, destination: File) {
        destination.parentFile?.mkdirs()
        val shortCount = samples.size
        val totalAudioLen = shortCount * 2L
        val totalDataLen = totalAudioLen + 36L
        val byteRate = SAMPLE_RATE * 1 * 2

        val header = ByteArray(44)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray())
        buf.putInt(totalDataLen.toInt())
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray())
        buf.putInt(16)
        buf.putShort(1) // PCM
        buf.putShort(1) // Mono
        buf.putInt(SAMPLE_RATE)
        buf.putInt(byteRate)
        buf.putShort(2) // BlockAlign
        buf.putShort(16) // BitsPerSample
        buf.put("data".toByteArray())
        buf.putInt(totalAudioLen.toInt())

        FileOutputStream(destination).use { fos ->
            fos.write(header)
            val pcmBytes = ByteArray(shortCount * 2)
            val pcmBuf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                val clamped = (s * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                pcmBuf.putShort(clamped)
            }
            fos.write(pcmBytes)
        }
    }

    fun playAudioPreview(samples: FloatArray) {
        val shortCount = samples.size
        val shorts = ShortArray(shortCount)
        for (i in samples.indices) {
            shorts[i] = (samples[i] * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(shortCount * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(shorts, 0, shortCount)
        track.play()
    }
}
