package app.melova.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.melova.data.local.FollowStore
import app.melova.data.local.PracticeProgressStore
import app.melova.data.model.Course
import app.melova.data.model.Lesson
import app.melova.data.repository.CourseRepository
import app.melova.data.repository.RemoteCourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Course catalog, followed courses and practice progress; shared across the app's screens. */
class CourseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CourseRepository = RemoteCourseRepository()
    private val progressStore = PracticeProgressStore(application)
    private val followStore = FollowStore(application)

    private val _uiState = MutableStateFlow(
        CourseUiState(followedIds = followStore.followedIds())
    )
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** [silent] refreshes in the background without the pull-to-refresh spinner. */
    fun refresh(silent: Boolean = false) {
        if (!silent) _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repository.courses(getApplication(), forceRefresh = true) }
                .onSuccess { courses ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            courses = courses,
                            bestScores = progressStore.allBestScores()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (silent && it.courses.isNotEmpty()) {
                                it.errorMessage
                            } else {
                                error.message ?: "خطا در دریافت دوره‌ها"
                            }
                        )
                    }
                }
        }
    }

    fun refreshProgress() {
        _uiState.update { it.copy(bestScores = progressStore.allBestScores()) }
    }

    fun setFollowed(courseId: String, followed: Boolean) {
        followStore.setFollowed(courseId, followed)
        _uiState.update { it.copy(followedIds = followStore.followedIds()) }
    }
}

data class CourseUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val courses: List<Course> = emptyList(),
    val followedIds: Set<String> = emptySet(),
    /** Keyed by [PracticeProgressStore.key]. */
    val bestScores: Map<String, Int> = emptyMap()
) {
    val followedCourses: List<Course>
        get() = courses.filter { it.id in followedIds }

    val suggestedCourses: List<Course>
        get() = courses.filter { it.id !in followedIds }

    fun isFollowed(courseId: String): Boolean = courseId in followedIds

    fun course(courseId: String): Course? = courses.firstOrNull { it.id == courseId }

    fun lesson(lessonId: String): Lesson? =
        courses.firstNotNullOfOrNull { course -> course.lessons.firstOrNull { it.id == lessonId } }

    fun bestScore(lessonId: String, sectionId: String): Int? =
        bestScores[PracticeProgressStore.key(lessonId, sectionId)]

    fun practicedSections(lesson: Lesson): Int =
        lesson.sections.count { bestScore(lesson.id, it.id) != null }

    fun practicedSections(course: Course): Int = course.lessons.sumOf { practicedSections(it) }

    fun availableSections(course: Course): Int =
        course.lessons.sumOf { lesson -> lesson.sections.count { it.isAvailable } }

    val totalSections: Int
        get() = followedCourses.sumOf { availableSections(it) }

    val totalPracticed: Int
        get() = followedCourses.sumOf { practicedSections(it) }

    val bestScoreOverall: Int?
        get() = bestScores.values.maxOrNull()
}
