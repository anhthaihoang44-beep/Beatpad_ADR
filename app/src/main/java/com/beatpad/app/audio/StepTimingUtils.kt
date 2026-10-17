package com.beatpad.app.audio

import com.beatpad.app.data.model.HitRating
import com.beatpad.app.data.model.LoopPattern
import com.beatpad.app.data.model.RecordingEvent
import kotlin.math.abs

object StepTimingUtils {

    const val PERFECT_WINDOW_MS = 60L
    const val GOOD_WINDOW_MS = 120L

    fun calculateStepDurationMs(bpm: Int): Double {
        val safeBpm = bpm.coerceIn(40, 240)
        // 1 beat = 60000 / BPM ms. 1 step (16th note) = beat / 4
        return (60_000.0 / safeBpm) / 4.0
    }

    fun calculateStepDurationNanos(bpm: Int): Long {
        return (calculateStepDurationMs(bpm) * 1_000_000.0).toLong()
    }

    fun calculateSwingOffsetMs(stepIndex: Int, stepDurationMs: Double, swingPercent: Int): Double {
        val clampedSwing = swingPercent.coerceIn(0, 50)
        // Apply swing delay to odd steps (16th note offbeats: 1, 3, 5, 7...)
        return if (stepIndex % 2 != 0) {
            (clampedSwing / 100.0) * (stepDurationMs * 0.5)
        } else {
            0.0
        }
    }

    fun calculateStepTimeMs(stepIndex: Int, bpm: Int, swingPercent: Int): Long {
        val baseStepDuration = calculateStepDurationMs(bpm)
        val regularTime = stepIndex * baseStepDuration
        val swingOffset = calculateSwingOffsetMs(stepIndex, baseStepDuration, swingPercent)
        return (regularTime + swingOffset).toLong()
    }

    fun evaluateHit(tapTimeMs: Long, targetTimeMs: Long): HitRating {
        val delta = abs(tapTimeMs - targetTimeMs)
        return when {
            delta <= PERFECT_WINDOW_MS -> HitRating.PERFECT
            delta <= GOOD_WINDOW_MS -> HitRating.GOOD
            else -> HitRating.MISS
        }
    }

    fun generateEventsForBars(pattern: LoopPattern, totalBars: Int = 4): List<RecordingEvent> {
        val events = mutableListOf<RecordingEvent>()
        val stepDurationMs = calculateStepDurationMs(pattern.bpm)
        val barDurationMs = stepDurationMs * LoopPattern.STEP_COUNT

        for (bar in 0 until totalBars) {
            val barStartMs = bar * barDurationMs
            for (step in 0 until LoopPattern.STEP_COUNT) {
                val stepOffsetMs = calculateStepTimeMs(step, pattern.bpm, pattern.swingPercent)
                val hitTimeMs = (barStartMs + stepOffsetMs).toLong()
                val releaseTimeMs = (hitTimeMs + stepDurationMs).toLong()

                for (padIndex in 0 until LoopPattern.PAD_COUNT) {
                    if (pattern.isStepActive(padIndex, step)) {
                        events.add(
                            RecordingEvent(
                                padIndex = padIndex,
                                pressTimeMs = hitTimeMs,
                                releaseTimeMs = releaseTimeMs,
                                side = "SIDE_A",
                                bpm = pattern.bpm
                            )
                        )
                    }
                }
            }
        }
        return events.sortedBy { it.pressTimeMs }
    }
}
