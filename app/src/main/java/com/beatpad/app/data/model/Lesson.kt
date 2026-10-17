package com.beatpad.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class HitRating {
    PERFECT,
    GOOD,
    MISS
}

@Serializable
data class LessonHit(
    val id: Int,
    val padIndex: Int,
    val timeMs: Long,
    val durationMs: Long = 200L
)

@Serializable
data class Lesson(
    val id: String,
    val packId: String,
    val title: String,
    val description: String,
    val bpm: Int,
    val durationBars: Int = 2,
    val hits: List<LessonHit> = emptyList()
)

data class LessonResult(
    val lessonId: String,
    val totalHits: Int,
    val perfectHits: Int,
    val goodHits: Int,
    val missHits: Int,
    val accuracyPercent: Int,
    val stars: Int
) {
    companion object {
        fun calculate(
            lessonId: String,
            totalHits: Int,
            perfectHits: Int,
            goodHits: Int,
            missHits: Int
        ): LessonResult {
            if (totalHits <= 0) {
                return LessonResult(lessonId, 0, 0, 0, 0, 0, 0)
            }
            val scorePoints = (perfectHits * 100) + (goodHits * 60)
            val maxPoints = totalHits * 100
            val accuracy = ((scorePoints.toDouble() / maxPoints.toDouble()) * 100).toInt().coerceIn(0, 100)
            val stars = when {
                accuracy >= 85 -> 3
                accuracy >= 65 -> 2
                accuracy >= 40 -> 1
                else -> 0
            }
            return LessonResult(
                lessonId = lessonId,
                totalHits = totalHits,
                perfectHits = perfectHits,
                goodHits = goodHits,
                missHits = missHits,
                accuracyPercent = accuracy,
                stars = stars
            )
        }
    }
}
