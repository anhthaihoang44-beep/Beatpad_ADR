package com.beatpad.app

import com.beatpad.app.audio.StepTimingUtils
import com.beatpad.app.data.model.HitRating
import com.beatpad.app.data.model.LessonResult
import org.junit.Assert.assertEquals
import org.junit.Test

class TimingWindowScoringTest {

    @Test
    fun testPerfectHitWindow() {
        val target = 1000L
        assertEquals(HitRating.PERFECT, StepTimingUtils.evaluateHit(1000L, target))
        assertEquals(HitRating.PERFECT, StepTimingUtils.evaluateHit(1060L, target))
        assertEquals(HitRating.PERFECT, StepTimingUtils.evaluateHit(940L, target))
        assertEquals(HitRating.PERFECT, StepTimingUtils.evaluateHit(975L, target))
    }

    @Test
    fun testGoodHitWindow() {
        val target = 1000L
        assertEquals(HitRating.GOOD, StepTimingUtils.evaluateHit(1061L, target))
        assertEquals(HitRating.GOOD, StepTimingUtils.evaluateHit(939L, target))
        assertEquals(HitRating.GOOD, StepTimingUtils.evaluateHit(1120L, target))
        assertEquals(HitRating.GOOD, StepTimingUtils.evaluateHit(880L, target))
    }

    @Test
    fun testMissHitWindow() {
        val target = 1000L
        assertEquals(HitRating.MISS, StepTimingUtils.evaluateHit(1121L, target))
        assertEquals(HitRating.MISS, StepTimingUtils.evaluateHit(879L, target))
        assertEquals(HitRating.MISS, StepTimingUtils.evaluateHit(1500L, target))
        assertEquals(HitRating.MISS, StepTimingUtils.evaluateHit(200L, target))
    }

    @Test
    fun testAccuracyAndStarCalculation() {
        // 10 hits: 9 perfect (900), 1 good (60) = 960 / 1000 = 96% -> 3 stars
        val result3Stars = LessonResult.calculate("test_1", 10, 9, 1, 0)
        assertEquals(96, result3Stars.accuracyPercent)
        assertEquals(3, result3Stars.stars)

        // 10 hits: 7 perfect (700), 1 good (60), 2 miss = 760 / 1000 = 76% -> 2 stars
        val result2Stars = LessonResult.calculate("test_2", 10, 7, 1, 2)
        assertEquals(76, result2Stars.accuracyPercent)
        assertEquals(2, result2Stars.stars)

        // 10 hits: 3 perfect (300), 2 good (120), 5 miss = 420 / 1000 = 42% -> 1 star
        val result1Star = LessonResult.calculate("test_3", 10, 3, 2, 5)
        assertEquals(42, result1Star.accuracyPercent)
        assertEquals(1, result1Star.stars)

        // 10 hits: 10 miss = 0% -> 0 stars
        val result0Stars = LessonResult.calculate("test_4", 10, 0, 0, 10)
        assertEquals(0, result0Stars.accuracyPercent)
        assertEquals(0, result0Stars.stars)

        // 0 hits edge case
        val resultEmpty = LessonResult.calculate("test_empty", 0, 0, 0, 0)
        assertEquals(0, resultEmpty.accuracyPercent)
        assertEquals(0, resultEmpty.stars)
    }
}
