package com.beatpad.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRestoreTest {

    @Test
    fun testZipArchiveCreationAndExtraction() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            // Entry 1: metadata
            zos.putNextEntry(ZipEntry("tracks_metadata.json"))
            zos.write("[{\"id\":1,\"name\":\"Test Beat\",\"durationMs\":4000}]".toByteArray())
            zos.closeEntry()

            // Entry 2: sample
            zos.putNextEntry(ZipEntry("custom_packs/my_pads.json"))
            zos.write("{\"id\":\"custom_my_pads\",\"name\":\"My Pads\"}".toByteArray())
            zos.closeEntry()
        }

        val zipBytes = out.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        // Read back
        val extractedEntries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val content = zis.bufferedReader().readText()
                extractedEntries[entry.name] = content
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertEquals(2, extractedEntries.size)
        assertTrue(extractedEntries.containsKey("tracks_metadata.json"))
        assertTrue(extractedEntries.containsKey("custom_packs/my_pads.json"))
        assertTrue(extractedEntries["tracks_metadata.json"]!!.contains("Test Beat"))
        assertTrue(extractedEntries["custom_packs/my_pads.json"]!!.contains("My Pads"))
    }
}
