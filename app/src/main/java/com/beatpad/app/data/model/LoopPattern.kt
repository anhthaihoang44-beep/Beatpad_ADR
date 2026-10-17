package com.beatpad.app.data.model

data class LoopPattern(
    val packId: String,
    val bpm: Int,
    val swingPercent: Int = 0,
    // Map of padIndex (0..11) to set of active step indices (0..15)
    val activeSteps: Map<Int, Set<Int>> = emptyMap()
) {
    companion object {
        const val STEP_COUNT = 16
        const val PAD_COUNT = 12

        fun empty(packId: String, bpm: Int): LoopPattern {
            return LoopPattern(
                packId = packId,
                bpm = bpm,
                swingPercent = 0,
                activeSteps = (0 until PAD_COUNT).associateWith { emptySet() }
            )
        }
    }

    fun isStepActive(padIndex: Int, step: Int): Boolean {
        return activeSteps[padIndex]?.contains(step) == true
    }

    fun toggleStep(padIndex: Int, step: Int): LoopPattern {
        val currentSet = activeSteps[padIndex] ?: emptySet()
        val newSet = if (currentSet.contains(step)) {
            currentSet - step
        } else {
            currentSet + step
        }
        val newMap = activeSteps.toMutableMap()
        newMap[padIndex] = newSet
        return copy(activeSteps = newMap)
    }

    fun clear(): LoopPattern {
        return copy(activeSteps = (0 until PAD_COUNT).associateWith { emptySet() })
    }

    fun hasAnyActiveSteps(): Boolean {
        return activeSteps.values.any { it.isNotEmpty() }
    }
}
