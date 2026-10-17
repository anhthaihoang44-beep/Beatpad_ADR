package com.beatpad.app.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.beatpad.app.data.model.PackManifest
import com.beatpad.app.data.model.PadInfo
import com.beatpad.app.data.model.SoundPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface PackRepository {
    suspend fun getPack(packId: String): Result<SoundPack>
    suspend fun getAllPacks(): List<SoundPack>
}

class AssetPackRepository(
    private val context: Context,
    private val customPackManager: CustomPackManager = CustomPackManager(context)
) : PackRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val packOrder = listOf(
        "trap",
        "cyber_edm",
        "deep_house",
        "cyber_dubstep",
        "future_bass",
        "lofi_chill",
        "neuro_dnb",
        "retro_electro"
    )

    override suspend fun getPack(packId: String): Result<SoundPack> = withContext(Dispatchers.IO) {
        if (packId == CustomPackManager.CUSTOM_PACK_ID || packId == "my_pads") {
            return@withContext Result.success(customPackManager.loadCustomPack())
        }
        runCatching {
            val content = try {
                context.assets.open("packs/$packId/pack.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                context.assets.open("packs/$packId.json").bufferedReader().use { it.readText() }
            }
            val manifest = json.decodeFromString<PackManifest>(content)
            mapManifestToDomain(manifest)
        }
    }

    override suspend fun getAllPacks(): List<SoundPack> = withContext(Dispatchers.IO) {
        val packs = packOrder.mapNotNull { id ->
            getPack(id).getOrNull()
        }.toMutableList()

        // Include custom "My Pads" pack
        runCatching {
            val custom = customPackManager.loadCustomPack()
            packs.add(custom)
        }
        packs
    }

    private fun mapManifestToDomain(manifest: PackManifest): SoundPack {
        val pads = manifest.pads.map { p ->
            val color = runCatching {
                Color(android.graphics.Color.parseColor(p.color))
            }.getOrDefault(p.family.color())

            PadInfo(
                index = p.actualIndex,
                name = p.name,
                family = p.family,
                fileA = p.resolveFileA(manifest.id),
                fileB = p.resolveFileB(manifest.id),
                color = color,
                mode = p.mode
            )
        }

        val demo = if (manifest.demoSample.isNotEmpty()) {
            manifest.demoSample
        } else {
            pads.firstOrNull { it.fileA.isNotEmpty() }?.fileA ?: ""
        }

        return SoundPack(
            id = manifest.id,
            name = manifest.name,
            genre = manifest.genre,
            bpm = manifest.bpm,
            key = manifest.key,
            producer = manifest.producer,
            isNew = manifest.isNew,
            colorPrimary = manifest.colorPrimary,
            colorSecondary = manifest.colorSecondary,
            demoSample = demo,
            pads = pads
        )
    }
}
