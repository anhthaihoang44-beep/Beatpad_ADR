package com.beatpad.app.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.beatpad.app.data.model.InstrumentFamily
import com.beatpad.app.data.model.PadInfo
import com.beatpad.app.data.model.PadMode
import com.beatpad.app.data.model.SoundPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CustomPackManager(private val context: Context) {

    private val packDir = File(context.filesDir, "custom_packs")
    private val samplesDir = File(context.filesDir, "custom_samples")
    private val packFile = File(packDir, "my_pads.json")

    init {
        packDir.mkdirs()
        samplesDir.mkdirs()
    }

    suspend fun loadCustomPack(): SoundPack = withContext(Dispatchers.IO) {
        if (!packFile.exists()) {
            createDefaultCustomPack()
        }
        runCatching {
            val jsonText = packFile.readText()
            val obj = JSONObject(jsonText)
            val padsArray = obj.getJSONArray("pads")
            val padList = mutableListOf<PadInfo>()

            for (i in 0 until padsArray.length()) {
                val padObj = padsArray.getJSONObject(i)
                val familyStr = padObj.optString("family", "DRUMS")
                val family = runCatching { InstrumentFamily.valueOf(familyStr) }.getOrDefault(InstrumentFamily.DRUMS)
                val colorHex = padObj.optString("color", "#FF8A3D")
                val color = parseHexColor(colorHex)

                padList.add(
                    PadInfo(
                        index = padObj.getInt("id"),
                        name = padObj.optString("name", "Custom Pad ${padObj.getInt("id") + 1}"),
                        family = family,
                        fileA = padObj.optString("fileA", ""),
                        fileB = padObj.optString("fileB", ""),
                        color = color,
                        mode = PadMode.ONE_SHOT
                    )
                )
            }

            SoundPack(
                id = CUSTOM_PACK_ID,
                name = obj.optString("name", "My Pads"),
                genre = "Custom",
                bpm = obj.optInt("bpm", 130),
                key = "C Major",
                producer = "BeatPad Studio",
                isNew = false,
                colorPrimary = "#FFC83D",
                colorSecondary = "#5CE05C",
                demoSample = "",
                pads = padList
            )
        }.getOrElse {
            createDefaultCustomPack()
        }
    }

    suspend fun assignSampleToPad(
        padIndex: Int,
        sampleFile: File,
        padName: String,
        family: InstrumentFamily
    ): SoundPack = withContext(Dispatchers.IO) {
        val currentPack = loadCustomPack()
        val destFile = File(samplesDir, "pad_${padIndex}.wav")
        sampleFile.copyTo(destFile, overwrite = true)

        val updatedPads = currentPack.pads.map { pad ->
            if (pad.index == padIndex) {
                pad.copy(
                    name = padName.ifBlank { "Pad ${padIndex + 1}" },
                    family = family,
                    fileA = destFile.absolutePath,
                    color = family.color()
                )
            } else {
                pad
            }
        }

        val updatedPack = currentPack.copy(pads = updatedPads)
        saveCustomPackToFile(updatedPack)
        updatedPack
    }

    private fun createDefaultCustomPack(): SoundPack {
        val defaultPads = (0 until 12).map { i ->
            val family = when (i % 5) {
                0 -> InstrumentFamily.DRUMS
                1 -> InstrumentFamily.BASS
                2 -> InstrumentFamily.SYNTH
                3 -> InstrumentFamily.CHORDS
                else -> InstrumentFamily.VOCAL
            }
            PadInfo(
                index = i,
                name = "Mic Pad ${i + 1}",
                family = family,
                fileA = "",
                fileB = "",
                color = family.color(),
                mode = PadMode.ONE_SHOT
            )
        }

        val pack = SoundPack(
            id = CUSTOM_PACK_ID,
            name = "My Pads",
            genre = "Custom",
            bpm = 130,
            key = "C Major",
            producer = "BeatPad Studio",
            isNew = false,
            colorPrimary = "#FFC83D",
            colorSecondary = "#5CE05C",
            demoSample = "",
            pads = defaultPads
        )
        saveCustomPackToFile(pack)
        return pack
    }

    private fun saveCustomPackToFile(pack: SoundPack) {
        val obj = JSONObject()
        obj.put("id", pack.id)
        obj.put("name", pack.name)
        obj.put("genre", pack.genre)
        obj.put("bpm", pack.bpm)

        val padsArray = JSONArray()
        for (pad in pack.pads) {
            val padObj = JSONObject()
            padObj.put("id", pad.index)
            padObj.put("name", pad.name)
            padObj.put("family", pad.family.name)
            padObj.put("fileA", pad.fileA)
            padObj.put("fileB", pad.fileB)
            padObj.put("color", pad.colorPrimaryHex())
            padsArray.put(padObj)
        }
        obj.put("pads", padsArray)
        packFile.writeText(obj.toString())
    }

    fun getAllCustomFiles(): List<File> {
        val files = mutableListOf<File>()
        if (packFile.exists()) files.add(packFile)
        samplesDir.listFiles()?.forEach { if (it.isFile) files.add(it) }
        return files
    }

    private fun parseHexColor(hex: String): Color {
        return runCatching {
            val cleanHex = if (hex.startsWith("#")) hex.substring(1) else hex
            Color(cleanHex.toLong(16) or 0xFF000000)
        }.getOrDefault(Color(0xFFFF8A3D))
    }

    private fun PadInfo.colorPrimaryHex(): String {
        return when (family) {
            InstrumentFamily.DRUMS -> "#FF8A3D"
            InstrumentFamily.BASS -> "#FF4D8D"
            InstrumentFamily.SYNTH -> "#5CE05C"
            InstrumentFamily.CHORDS -> "#3DA5FF"
            else -> "#FFD83D"
        }
    }

    companion object {
        const val CUSTOM_PACK_ID = "custom_my_pads"
    }
}
