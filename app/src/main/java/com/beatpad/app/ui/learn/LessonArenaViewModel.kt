package com.beatpad.app.ui.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beatpad.app.audio.AudioEngine
import com.beatpad.app.audio.StepTimingUtils
import com.beatpad.app.data.model.HitRating
import com.beatpad.app.data.model.Lesson
import com.beatpad.app.data.model.LessonHit
import com.beatpad.app.data.model.LessonResult
import com.beatpad.app.data.model.SoundPack
import com.beatpad.app.data.repository.AssetLessonRepository
import com.beatpad.app.data.repository.LessonRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class LessonTab { LISTEN, PLAY }

data class LessonArenaUiState(
    val lesson: Lesson,
    val pack: SoundPack,
    val activeTab: LessonTab = LessonTab.LISTEN,
    val isPlaying: Boolean = false,
    val currentTimeMs: Long = 0L,
    val totalDurationMs: Long = 3000L,
    val activeLitPads: Set<Int> = emptySet(),
    val usedPadIndices: Set<Int> = emptySet(),
    val nextTargetHit: LessonHit? = null,
    val upcomingHitsPerPad: Map<Int, Int> = emptyMap(),
    val lastRating: HitRating? = null,
    val ratingTriggerCount: Int = 0,
    val perfectHits: Int = 0,
    val goodHits: Int = 0,
    val missHits: Int = 0,
    val runningAccuracy: Int = 100,
    val completedResult: LessonResult? = null
)

class LessonArenaViewModel(
    application: Application,
    val lesson: Lesson,
    val pack: SoundPack,
    private val audioEngine: AudioEngine,
    private val lessonRepository: LessonRepository = AssetLessonRepository(application)
) : AndroidViewModel(application) {

    private val totalDurationMs: Long = run {
        val maxHit = lesson.hits.maxOfOrNull { it.timeMs + it.durationMs } ?: 2000L
        val barMs = ((240_000.0 / lesson.bpm) * lesson.durationBars).toLong()
        maxOf(maxHit, barMs)
    }

    private val usedPads = lesson.hits.map { it.padIndex }.toSet()
    private val _uiState = MutableStateFlow(
        LessonArenaUiState(
            lesson = lesson,
            pack = pack,
            totalDurationMs = totalDurationMs,
            usedPadIndices = usedPads
        )
    )
    val uiState: StateFlow<LessonArenaUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private val hitJudgedMap = mutableMapOf<Int, HitRating>()
    private var sessionStartTimeMs: Long = 0L
    private var latencyOffsetMs: Int = 0

    init {
        viewModelScope.launch {
            com.beatpad.app.data.repository.UserPreferencesRepository(application).latencyOffsetMs.collect {
                latencyOffsetMs = it
            }
        }
        switchTab(LessonTab.LISTEN)
    }

    fun switchTab(tab: LessonTab) {
        stopSession()
        _uiState.value = _uiState.value.copy(
            activeTab = tab,
            isPlaying = false,
            currentTimeMs = 0L,
            activeLitPads = emptySet(),
            nextTargetHit = null,
            lastRating = null,
            perfectHits = 0,
            goodHits = 0,
            missHits = 0,
            runningAccuracy = 100,
            completedResult = null
        )
        hitJudgedMap.clear()
        startSession()
    }

    fun retry() = switchTab(_uiState.value.activeTab)

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) stopSession() else startSession()
    }

    private fun startSession() {
        playbackJob?.cancel()
        hitJudgedMap.clear()
        sessionStartTimeMs = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            isPlaying = true,
            currentTimeMs = 0L,
            completedResult = null,
            perfectHits = 0,
            goodHits = 0,
            missHits = 0,
            runningAccuracy = 100
        )

        playbackJob = viewModelScope.launch {
            val isListenMode = _uiState.value.activeTab == LessonTab.LISTEN
            val playedHits = mutableSetOf<Int>()

            while (isActive && _uiState.value.isPlaying) {
                val elapsed = System.currentTimeMillis() - sessionStartTimeMs
                _uiState.value = _uiState.value.copy(currentTimeMs = elapsed)

                if (isListenMode) {
                    val litPads = mutableSetOf<Int>()
                    for (hit in lesson.hits) {
                        if (elapsed in hit.timeMs until (hit.timeMs + hit.durationMs)) {
                            litPads.add(hit.padIndex)
                        }
                        if (elapsed >= hit.timeMs && !playedHits.contains(hit.id)) {
                            playedHits.add(hit.id)
                            audioEngine.onPadDown(hit.padIndex)
                        }
                    }
                    _uiState.value = _uiState.value.copy(activeLitPads = litPads)
                } else {
                    val upcoming = lesson.hits.filter { hit ->
                        !hitJudgedMap.containsKey(hit.id) && hit.timeMs >= elapsed - StepTimingUtils.GOOD_WINDOW_MS
                    }
                    val nextHit = upcoming.minByOrNull { it.timeMs }
                    val counts = upcoming.groupingBy { it.padIndex }.eachCount()

                    for (hit in lesson.hits) {
                        if (!hitJudgedMap.containsKey(hit.id) && elapsed > hit.timeMs + StepTimingUtils.GOOD_WINDOW_MS) {
                            recordRating(hit.id, HitRating.MISS)
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        nextTargetHit = nextHit,
                        upcomingHitsPerPad = counts
                    )
                }

                if (elapsed >= totalDurationMs + 300L) {
                    if (isListenMode) {
                        sessionStartTimeMs = System.currentTimeMillis()
                        playedHits.clear()
                    } else {
                        finishPlaySession()
                        break
                    }
                }
                delay(20L)
            }
        }
    }

    fun onPadDown(padIndex: Int) {
        audioEngine.onPadDown(padIndex)
        if (_uiState.value.activeTab != LessonTab.PLAY || !_uiState.value.isPlaying) return

        val elapsed = _uiState.value.currentTimeMs
        val adjustedElapsed = elapsed - latencyOffsetMs
        val candidate = lesson.hits.filter { hit ->
            hit.padIndex == padIndex && !hitJudgedMap.containsKey(hit.id) &&
                    abs(adjustedElapsed - hit.timeMs) <= 250L
        }.minByOrNull { abs(adjustedElapsed - it.timeMs) }

        if (candidate != null) {
            val rating = StepTimingUtils.evaluateHit(adjustedElapsed, candidate.timeMs)
            recordRating(candidate.id, rating)
        } else {
            recordRating(-1, HitRating.MISS)
        }

        val currentLit = _uiState.value.activeLitPads
        _uiState.value = _uiState.value.copy(activeLitPads = currentLit + padIndex)
        viewModelScope.launch {
            delay(120L)
            _uiState.value = _uiState.value.copy(
                activeLitPads = _uiState.value.activeLitPads - padIndex
            )
        }
    }

    fun onPadUp(padIndex: Int) {
        audioEngine.onPadUp(padIndex)
    }

    private fun recordRating(hitId: Int, rating: HitRating) {
        if (hitId >= 0) hitJudgedMap[hitId] = rating

        val perf = if (rating == HitRating.PERFECT) _uiState.value.perfectHits + 1 else _uiState.value.perfectHits
        val good = if (rating == HitRating.GOOD) _uiState.value.goodHits + 1 else _uiState.value.goodHits
        val miss = if (rating == HitRating.MISS) _uiState.value.missHits + 1 else _uiState.value.missHits

        val totalJudged = perf + good + miss
        val accuracy = if (totalJudged > 0) {
            val score = (perf * 100) + (good * 60)
            ((score.toDouble() / (totalJudged * 100)) * 100).toInt().coerceIn(0, 100)
        } else 100

        _uiState.value = _uiState.value.copy(
            lastRating = rating,
            ratingTriggerCount = _uiState.value.ratingTriggerCount + 1,
            perfectHits = perf,
            goodHits = good,
            missHits = miss,
            runningAccuracy = accuracy
        )
    }

    private fun finishPlaySession() {
        stopSession()
        for (hit in lesson.hits) {
            if (!hitJudgedMap.containsKey(hit.id)) {
                recordRating(hit.id, HitRating.MISS)
            }
        }

        val total = lesson.hits.size
        val perf = _uiState.value.perfectHits
        val good = _uiState.value.goodHits
        val miss = (total - perf - good).coerceAtLeast(0)

        val result = LessonResult.calculate(
            lessonId = lesson.id,
            totalHits = total,
            perfectHits = perf,
            goodHits = good,
            missHits = miss
        )

        _uiState.value = _uiState.value.copy(
            completedResult = result,
            isPlaying = false
        )

        viewModelScope.launch {
            lessonRepository.saveLessonStars(lesson.id, result.stars)
        }
    }

    private fun stopSession() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            activeLitPads = emptySet(),
            nextTargetHit = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }

    class Factory(
        private val application: Application,
        private val lesson: Lesson,
        private val pack: SoundPack,
        private val audioEngine: AudioEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LessonArenaViewModel(application, lesson, pack, audioEngine) as T
        }
    }
}
