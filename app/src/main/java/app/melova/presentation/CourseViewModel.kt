package app.melova.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.melova.data.local.PracticeProgressStore
import app.melova.data.model.Lesson
import app.melova.data.repository.CourseRepository
import app.melova.data.repository.RemoteCourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lesson catalog and the student's progress; shared by the home and lesson screens. */
class CourseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CourseRepository = RemoteCourseRepository()
    private val progressStore = PracticeProgressStore(application)

    private val _uiState = MutableStateFlow(CourseUiState())
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repository.lessons(getApplication(), forceRefresh = true) }
                .onSuccess { lessons ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lessons = lessons,
                            bestScores = progressStore.allBestScores()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "خطا در دریافت درس‌ها"
                        )
                    }
                }
        }
    }

    fun refreshProgress() {
        _uiState.update { it.copy(bestScores = progressStore.allBestScores()) }
    }
}

data class CourseUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val lessons: List<Lesson> = emptyList(),
    /** Keyed by [PracticeProgressStore.key]. */
    val bestScores: Map<String, Int> = emptyMap()
) {
    fun lesson(lessonId: String): Lesson? = lessons.firstOrNull { it.id == lessonId }

    fun bestScore(lessonId: String, sectionId: String): Int? =
        bestScores[PracticeProgressStore.key(lessonId, sectionId)]

    fun practicedSections(lesson: Lesson): Int =
        lesson.sections.count { bestScore(lesson.id, it.id) != null }

    val totalSections: Int
        get() = lessons.sumOf { lesson -> lesson.sections.count { it.isAvailable } }

    val totalPracticed: Int
        get() = lessons.sumOf { practicedSections(it) }
}
