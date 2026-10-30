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
import app.melova.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Teachers and lessons, followed teachers and practice progress; shared across the app's screens. */
class CatalogViewModel : ViewModel() {
    private val repository: LessonRepository = RemoteLessonRepository()
    private val studentRepository = StudentRepository(appContext)
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
        studentRepository.restoreSession()
        refresh()
    }

    /** [silent] refreshes in the background without the pull-to-refresh spinner. */
    fun refresh(silent: Boolean = false) {
        if (!silent) _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            moveDeviceFollows()
            runCatching { repository.catalog(appContext, forceRefresh = true) }
                .onSuccess { catalog ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            catalog = catalog,
                            followedIds = catalog.followedTeacherIds ?: it.followedIds,
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

    /** The button answers at once, and goes back if the server didn't take it. */
    fun setFollowed(teacherId: String, followed: Boolean) {
        val before = _uiState.value.followedIds
        _uiState.update { it.copy(followedIds = if (followed) before + teacherId else before - teacherId) }
        viewModelScope.launch {
            runCatching {
                if (followed) studentRepository.follow(setOf(teacherId)) else studentRepository.unfollow(teacherId)
            }
                .onSuccess { ids -> _uiState.update { it.copy(followedIds = ids) } }
                .onFailure { error ->
                    _uiState.update { it.copy(followedIds = before, message = error.message ?: "دنبال کردن ممکن نشد.") }
                }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /** Follows made before the account held them; if this fails, the next refresh tries again. */
    private suspend fun moveDeviceFollows() {
        val onDevice = followStore.followedIds()
        if (onDevice.isEmpty()) return
        runCatching { studentRepository.follow(onDevice) }.onSuccess { followStore.clear() }
    }
}

data class CatalogUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** For a snackbar, unlike [errorMessage] which stands in for the catalog. */
    val message: String? = null,
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

    /** What Home shows: lessons the student has, paid for and waiting on, or has sung in. */
    val myLessons: List<Lesson>
        get() = catalog.lessons.filter {
            it.isOwned == true || it.purchaseStatus != null || (it.isOpen && practicedSections(it) > 0)
        }

    /** Every lesson on offer, the followed teachers' first. */
    val exploreLessons: List<Lesson>
        get() = catalog.lessons.filter { it.published }.sortedBy { it.teacherId !in followedIds }

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

    /** The student's lessons that can be sung in now. */
    private val practiceLessons: List<Lesson>
        get() = myLessons.filter { it.isOpen && it.availableSections > 0 }

    val totalSections: Int
        get() = practiceLessons.sumOf { it.availableSections }

    val totalPracticed: Int
        get() = practiceLessons.sumOf { practicedSections(it) }

    /**
     * The one lesson to offer first: the one the student last sang in while it is
     * unfinished, else their first unfinished lesson.
     */
    val nextLesson: Lesson?
        get() {
            val playable = practiceLessons
            playable.firstOrNull { it.id == lastLessonId && !isDone(it) }?.let { return it }
            return playable.firstOrNull { !isDone(it) }
        }

    /** Every word of [query] has to appear, in any order, so «مرداب گوگوش» finds either way round. */
    fun searchLessons(query: String): List<Lesson> = exploreLessons.filter { lesson ->
        with(lesson) { "$title $subtitle $teacher $level $description" }.matches(query)
    }

    fun searchTeachers(query: String): List<Teacher> = teachers.filter { "${it.name} ${it.bio}".matches(query) }

    private fun String.matches(query: String): Boolean {
        val text = searchKey()
        return query.searchKey().split(' ').filter { it.isNotEmpty() }.all { it in text }
    }
}

/** Lowercase, with the letter forms keyboards mix up made one, so «علي» finds «علی». */
private fun String.searchKey(): String = buildString {
    for (char in this@searchKey.lowercase()) {
        when (char) {
            'ي', 'ى' -> append('ی')
            'ك' -> append('ک')
            'ة' -> append('ه')
            'أ', 'إ', 'آ' -> append('ا')
            '\u200C' -> append(' ')
            in '\u064B'..'\u0652' -> Unit
            else -> append(char)
        }
    }
}
