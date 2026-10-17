package com.beatpad.app.ui.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beatpad.app.data.model.Lesson
import com.beatpad.app.data.repository.AssetLessonRepository
import com.beatpad.app.data.repository.LessonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LessonItemUi(
    val lesson: Lesson,
    val stars: Int = 0
)

data class LearnUiState(
    val lessons: List<LessonItemUi> = emptyList(),
    val selectedFilter: String = "ALL",
    val isLoading: Boolean = true
)

class LearnViewModel(
    application: Application,
    private val lessonRepository: LessonRepository = AssetLessonRepository(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    init {
        loadLessons()
    }

    fun loadLessons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val rawLessons = lessonRepository.getLessons()
            val items = rawLessons.map { lesson ->
                val stars = lessonRepository.getLessonStars(lesson.id).first()
                LessonItemUi(lesson = lesson, stars = stars)
            }
            _uiState.value = _uiState.value.copy(
                lessons = items,
                isLoading = false
            )
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LearnViewModel(application) as T
        }
    }
}
