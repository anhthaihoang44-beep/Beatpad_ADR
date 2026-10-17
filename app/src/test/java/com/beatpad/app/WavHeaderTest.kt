package com.beatpad.app

import com.beatpad.app.util.WavUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavHeaderTest {

    @Test
    fun testWavHeaderFormat() {
        val tempFile = File.createTempFile("test_header", ".wav")
        try {
            val audioLength = 1000
            val sampleRate = 44100
            val channels = 1
            val bitsPerSample = 16

            FileOutputStream(tempFile).use { fos ->
                WavUtils.writeWavHeader(fos, audioLength, sampleRate, channels, bitsPerSample)
            }

            val bytes = tempFile.readBytes()
            assertEquals(44, bytes.size)

            // Verify 'RIFF' chunk descriptor
            assertEquals('R'.code.toByte(), bytes[0])
            assertEquals('I'.code.toByte(), bytes[1])
            assertEquals('F'.code.toByte(), bytes[2])
            assertEquals('F'.code.toByte(), bytes[3])

            // Verify 'WAVE' format
            assertEquals('W'.code.toByte(), bytes[8])
            assertEquals('A'.code.toByte(), bytes[9])
            assertEquals('V'.code.toByte(), bytes[10])
            assertEquals('E'.code.toByte(), bytes[11])

            // Verify 'data' chunk
            assertEquals('d'.code.toByte(), bytes[36])
            assertEquals('a'.code.toByte(), bytes[37])
            assertEquals('t'.code.toByte(), bytes[38])
            assertEquals('a'.code.toByte(), bytes[39])

            // Verify data length little endian
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val writtenAudioLength = buffer.getInt(40)
            assertEquals(audioLength, writtenAudioLength)
        } finally {
            tempFile.delete()
        }
    }
}
