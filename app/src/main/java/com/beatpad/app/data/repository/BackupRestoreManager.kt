package com.beatpad.app.data.repository

import android.content.Context
import com.beatpad.app.data.local.BeatPadDatabase
import com.beatpad.app.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRestoreManager(private val context: Context) {

    private val trackDao = BeatPadDatabase.getDatabase(context).trackDao()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exportBackup(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var exportedItems = 0
            val tracks = trackDao.getAllTracks().first()
            val customPadsDir = File(context.filesDir, "custom_packs")
            val customSamplesDir = File(context.filesDir, "custom_samples")

            ZipOutputStream(outputStream).use { zos ->
                // 1. Tracks Metadata
                val metadataJson = json.encodeToString(tracks)
                zos.putNextEntry(ZipEntry("tracks_metadata.json"))
                zos.write(metadataJson.toByteArray())
                zos.closeEntry()
                exportedItems++

                // 2. Track WAV Files
                for (track in tracks) {
                    val file = File(track.filePath)
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry("recordings/${file.name}"))
                        file.inputStream().use { stream -> stream.copyTo(zos) }
                        zos.closeEntry()
                        exportedItems++
                    }
                }

                // 3. Custom Pads Pack JSON
                val myPadsFile = File(customPadsDir, "my_pads.json")
                if (myPadsFile.exists()) {
                    zos.putNextEntry(ZipEntry("custom_packs/my_pads.json"))
                    myPadsFile.inputStream().use { stream -> stream.copyTo(zos) }
                    zos.closeEntry()
                    exportedItems++
                }

                // 4. Custom Samples
                customSamplesDir.listFiles()?.forEach { sample ->
                    if (sample.isFile) {
                        zos.putNextEntry(ZipEntry("custom_samples/${sample.name}"))
                        sample.inputStream().use { stream -> stream.copyTo(zos) }
                        zos.closeEntry()
                        exportedItems++
                    }
                }
            }
            exportedItems
        }
    }

    suspend fun importBackup(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var restoredCount = 0
            val recDir = File(context.filesDir, "recordings").apply { mkdirs() }
            val customPadsDir = File(context.filesDir, "custom_packs").apply { mkdirs() }
            val customSamplesDir = File(context.filesDir, "custom_samples").apply { mkdirs() }

            var tracksMetadataJson: String? = null

            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "tracks_metadata.json" -> {
                            tracksMetadataJson = zis.bufferedReader().readText()
                        }
                        name.startsWith("recordings/") -> {
                            val fileName = name.substringAfter("recordings/")
                            if (fileName.isNotBlank()) {
                                val dest = File(recDir, fileName)
                                dest.outputStream().use { stream -> zis.copyTo(stream) }
                                restoredCount++
                            }
                        }
                        name.startsWith("custom_packs/") -> {
                            val fileName = name.substringAfter("custom_packs/")
                            if (fileName.isNotBlank()) {
                                val dest = File(customPadsDir, fileName)
                                dest.outputStream().use { stream -> zis.copyTo(stream) }
                                restoredCount++
                            }
                        }
                        name.startsWith("custom_samples/") -> {
                            val fileName = name.substringAfter("custom_samples/")
                            if (fileName.isNotBlank()) {
                                val dest = File(customSamplesDir, fileName)
                                dest.outputStream().use { stream -> zis.copyTo(stream) }
                                restoredCount++
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Restore Database Tracks
            tracksMetadataJson?.let { rawJson ->
                val tracks = json.decodeFromString<List<TrackEntity>>(rawJson)
                for (track in tracks) {
                    val fixedPath = File(recDir, File(track.filePath).name).absolutePath
                    val updatedTrack = track.copy(filePath = fixedPath)
                    trackDao.insertTrack(updatedTrack)
                    restoredCount++
                }
            }
            restoredCount
        }
    }
}
