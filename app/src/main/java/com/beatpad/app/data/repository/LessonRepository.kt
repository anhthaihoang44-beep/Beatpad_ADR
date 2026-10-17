package com.beatpad.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.beatpad.app.data.model.Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

interface LessonRepository {
    suspend fun getLessons(): List<Lesson>
    suspend fun getLessonsForPack(packId: String): List<Lesson>
    suspend fun getLesson(lessonId: String): Lesson?
    fun getLessonStars(lessonId: String): Flow<Int>
    suspend fun saveLessonStars(lessonId: String, stars: Int)
}

class AssetLessonRepository(private val context: Context) : LessonRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private var cachedLessons: List<Lesson>? = null

    override suspend fun getLessons(): List<Lesson> = withContext(Dispatchers.IO) {
        cachedLessons?.let { return@withContext it }
        val loaded = mutableListOf<Lesson>()
        val lessonFiles = listOf(
            "lessons/trap_1.json",
            "lessons/trap_2.json",
            "lessons/trap_3.json",
            "lessons/house_1.json",
            "lessons/house_2.json",
            "lessons/house_3.json"
        )

        for (file in lessonFiles) {
            runCatching {
                context.assets.open(file).use { stream ->
                    val text = InputStreamReader(stream).readText()
                    val lesson = json.decodeFromString<Lesson>(text)
                    loaded.add(lesson)
                }
            }
        }
        cachedLessons = loaded
        loaded
    }

    override suspend fun getLessonsForPack(packId: String): List<Lesson> {
        val all = getLessons()
        // Map packId variations (neon_trap / trap, deep_house / house)
        val normalized = when (packId.lowercase()) {
            "trap" -> "neon_trap"
            "house" -> "deep_house"
            else -> packId.lowercase()
        }
        return all.filter {
            it.packId.lowercase() == normalized ||
                    it.packId.lowercase() == packId.lowercase()
        }
    }

    override suspend fun getLesson(lessonId: String): Lesson? {
        return getLessons().find { it.id == lessonId }
    }

    override fun getLessonStars(lessonId: String): Flow<Int> {
        val key = intPreferencesKey("stars_$lessonId")
        return context.userDataStore.data.map { preferences ->
            preferences[key] ?: 0
        }
    }

    override suspend fun saveLessonStars(lessonId: String, stars: Int) {
        val key = intPreferencesKey("stars_$lessonId")
        context.userDataStore.edit { preferences ->
            val current = preferences[key] ?: 0
            if (stars > current) {
                preferences[key] = stars
            }
        }
    }
}
