package app.melova.presentation

import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import androidx.lifecycle.viewModelScope
import app.melova.data.local.FollowStore
import app.melova.data.local.PracticeProgressStore
import app.melova.data.model.Catalog
import app.melova.data.model.Lesson
import app.melova.data.model.Teacher
import app.melova.data.repository.LessonRepository
import app.melova.data.repository.RemoteLessonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Teachers and lessons, followed teachers and practice progress; shared across the app's screens. */
class CatalogViewModel : ViewModel() {
    private val repository: LessonRepository = RemoteLessonRepository()
    private val progressStore = PracticeProgressStore(appContext)
    private val followStore = FollowStore(appContext)

    private val _uiState = MutableStateFlow(
        CatalogUiState(
            followedIds = followStore.followedIds(),
            lastLessonId = progressStore.lastLessonId()
        )
    )
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** [silent] refreshes in the background without the pull-to-refresh spinner. */
    fun refresh(silent: Boolean = false) {
        if (!silent) _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repository.catalog(appContext, forceRefresh = true) }
                .onSuccess { catalog ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            catalog = catalog,
                            bestScores = progressStore.allBestScores(),
                            lastLessonId = progressStore.lastLessonId()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (silent && it.catalog.lessons.isNotEmpty()) {
                                it.errorMessage
                            } else {
                                error.message ?: "خطا در دریافت درس‌ها"
                            }
                        )
                    }
                }
        }
    }

    fun refreshProgress() {
        _uiState.update {
            it.copy(bestScores = progressStore.allBestScores(), lastLessonId = progressStore.lastLessonId())
        }
    }

    fun setFollowed(teacherId: String, followed: Boolean) {
        followStore.setFollowed(teacherId, followed)
        _uiState.update { it.copy(followedIds = followStore.followedIds()) }
    }
}

data class CatalogUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val catalog: Catalog = Catalog(),
    /** Followed teacher ids. */
    val followedIds: Set<String> = emptySet(),
    /** Keyed by [PracticeProgressStore.key]. */
    val bestScores: Map<String, Int> = emptyMap(),
    val lastLessonId: String? = null
) {
    val teachers: List<Teacher>
        get() = catalog.teachers

    val hasContent: Boolean
        get() = catalog.lessons.isNotEmpty()

    val followedTeachers: List<Teacher>
        get() = teachers.filter { it.id in followedIds }

    val suggestedTeachers: List<Teacher>
        get() = teachers.filter { it.id !in followedIds && lessonsOf(it.id).isNotEmpty() }

    /** Lessons of the followed teachers, teacher by teacher. */
    val followedLessons: List<Lesson>
        get() = followedTeachers.flatMap { lessonsOf(it.id) }

    fun isFollowed(teacherId: String): Boolean = teacherId in followedIds

    fun teacher(teacherId: String): Teacher? = teachers.firstOrNull { it.id == teacherId }

    fun lessonsOf(teacherId: String): List<Lesson> = catalog.lessons.filter { it.teacherId == teacherId }

    fun lesson(lessonId: String): Lesson? = catalog.lessons.firstOrNull { it.id == lessonId }

    fun bestScore(lessonId: String, sectionId: String): Int? =
        bestScores[PracticeProgressStore.key(lessonId, sectionId)]

    fun practicedSections(lesson: Lesson): Int =
        lesson.sections.count { it.isAvailable && bestScore(lesson.id, it.id) != null }

    fun isDone(lesson: Lesson): Boolean =
        lesson.availableSections > 0 && practicedSections(lesson) >= lesson.availableSections

    fun practicedSections(teacherId: String): Int = lessonsOf(teacherId).sumOf { practicedSections(it) }

    fun availableSections(teacherId: String): Int = lessonsOf(teacherId).sumOf { it.availableSections }

    val totalSections: Int
        get() = followedLessons.sumOf { it.availableSections }

    val totalPracticed: Int
        get() = followedLessons.sumOf { practicedSections(it) }

    /**
     * The one lesson to offer first: the one the student last sang in while it is
     * unfinished, else the first unfinished lesson of a followed teacher.
     */
    val nextLesson: Lesson?
        get() {
            val playable = followedLessons.filter { it.availableSections > 0 }
            playable.firstOrNull { it.id == lastLessonId && !isDone(it) }?.let { return it }
            return playable.firstOrNull { !isDone(it) }
        }
}
