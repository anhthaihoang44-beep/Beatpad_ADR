package com.beatpad.app

import com.beatpad.app.data.model.InstrumentFamily
import com.beatpad.app.data.model.PackManifest
import com.beatpad.app.data.model.PadMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SoundPackManifestParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testParseValidSoundPackManifest() {
        val jsonString = """
            {
              "id": "test_pack",
              "name": "Test Pack",
              "genre": "Trap",
              "bpm": 140,
              "key": "F Minor",
              "producer": "BeatPad Studio",
              "isNew": true,
              "pads": [
                {
                  "index": 0,
                  "name": "Kick 808",
                  "family": "DRUMS",
                  "color": "#FF8A3D",
                  "mode": "ONE_SHOT",
                  "file": "pad_0.wav",
                  "fileB": "pad_0_b.wav"
                },
                {
                  "index": 5,
                  "name": "Sub 808",
                  "family": "BASS",
                  "color": "#FF4D8D",
                  "mode": "LOOP",
                  "file": "pad_4.wav",
                  "fileB": "pad_4_b.wav"
                }
              ]
            }
        """.trimIndent()

        val manifest = json.decodeFromString<PackManifest>(jsonString)

        assertEquals("test_pack", manifest.id)
        assertEquals("Test Pack", manifest.name)
        assertEquals("Trap", manifest.genre)
        assertEquals(140, manifest.bpm)
        assertEquals("F Minor", manifest.key)
        assertEquals(2, manifest.pads.size)

        val pad0 = manifest.pads[0]
        assertEquals(0, pad0.index)
        assertEquals("Kick 808", pad0.name)
        assertEquals(InstrumentFamily.DRUMS, pad0.family)
        assertEquals(PadMode.ONE_SHOT, pad0.mode)
        assertEquals("pad_0.wav", pad0.file)
        assertEquals("pad_0_b.wav", pad0.fileB)

        val pad1 = manifest.pads[1]
        assertEquals(5, pad1.index)
        assertEquals("Sub 808", pad1.name)
        assertEquals(InstrumentFamily.BASS, pad1.family)
        assertEquals(PadMode.LOOP, pad1.mode)
        assertEquals("pad_4.wav", pad1.file)
    }
}
