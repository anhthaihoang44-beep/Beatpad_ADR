package com.beatpad.app

import com.beatpad.app.data.model.InstrumentFamily
import com.beatpad.app.data.model.PackManifest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PackLibraryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val packIds = listOf(
        "trap",
        "cyber_edm",
        "deep_house",
        "cyber_dubstep",
        "future_bass",
        "lofi_chill",
        "neuro_dnb",
        "retro_electro"
    )

    @Test
    fun testAll8PacksExistAndParseCorrectly() {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        )
        val assetsDir = candidates.find { it.exists() } ?: File("src/main/assets")
        assertTrue("Assets directory should exist", assetsDir.exists())

        val packsDir = File(assetsDir, "packs")
        assertTrue("Packs directory should exist", packsDir.exists())

        for (packId in packIds) {
            val packFile = File(packsDir, "$packId/pack.json").takeIf { it.exists() }
                ?: File(packsDir, "$packId.json")

            assertTrue("Pack file for $packId must exist at $packFile", packFile.exists())
            val content = packFile.readText()
            val manifest = json.decodeFromString<PackManifest>(content)

            assertNotNull("Manifest for $packId should not be null", manifest)
            assertFalse("Pack ID should not be empty", manifest.id.isEmpty())
            assertFalse("Pack name should not be empty", manifest.name.isEmpty())
            assertTrue("Pack should have BPM > 60", manifest.bpm > 60)
            assertEquals("Pack $packId must have exactly 12 pads", 12, manifest.pads.size)

            // Verify each pad has valid file
            for (pad in manifest.pads) {
                val fileA = pad.resolveFileA(manifest.id)
                val sampleFile = File(assetsDir, fileA)
                assertTrue("Sample file $fileA for pad ${pad.name} in $packId must exist", sampleFile.exists())
            }
        }
    }
}
