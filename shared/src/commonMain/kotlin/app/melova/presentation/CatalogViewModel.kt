package app.melova.presentation

import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import androidx.lifecycle.viewModelScope
import app.melova.data.local.FollowStore
import app.melova.data.local.StudentSession
import app.melova.data.model.Catalog
import app.melova.data.model.Lesson
import app.melova.data.model.JournalSection
import app.melova.data.model.PracticeLevels
import app.melova.data.model.PracticeProgress
import app.melova.data.model.PracticeWeek
import app.melova.data.model.SectionLevel
import app.melova.data.model.LessonSection
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
    private val followStore = FollowStore(appContext)
    private val studentSession = StudentSession(appContext)

    private val _uiState = MutableStateFlow(
        CatalogUiState(
            catalogOwnerId = studentSession.studentId,
            followedIds = followStore.followedIds()
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
        refreshWeek()
        viewModelScope.launch {
            moveDeviceFollows()
            val studentId = studentSession.studentId
            val progress = runCatching { studentRepository.practiceProgress() }.getOrNull()
            runCatching { repository.catalog(appContext, forceRefresh = true) }
                .onSuccess { catalog ->
                    _uiState.update {
                        it.copy(
                            catalogOwnerId = studentId,
                            isLoading = false,
                            errorMessage = null,
                            catalog = catalog,
                            followedIds = catalog.followedTeacherIds ?: it.followedIds,
                            progress = progress ?: it.progress
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

    /**
     * Lessons and practice belong to a signed-in account: without one there are none, and the
     * practice read back is the new account's own. The refresh a new account triggers brings
     * its lessons.
     */
    fun onAccountChanged(signedIn: Boolean) {
        val studentId = studentSession.studentId
        _uiState.update {
            // A catalog fetched for another account still carries that account's lessons and follows.
            val stale = it.catalogOwnerId != studentId
            it.copy(
                signedIn = signedIn,
                catalogOwnerId = if (stale) studentId else it.catalogOwnerId,
                followedIds = if (stale) emptySet() else it.followedIds,
                catalog = if (!stale) it.catalog else it.catalog.copy(
                    lessons = it.catalog.lessons.map { lesson -> lesson.copy(isOwned = null, offer = null) }
                ),
                // Another account's practice isn't this one's; the refresh that follows brings its own.
                progress = if (stale) PracticeProgress() else it.progress,
                week = if (stale || !signedIn) null else it.week,
                journal = if (stale || !signedIn) null else it.journal
            )
        }
        refreshWeek()
    }

    fun refreshProgress() {
        viewModelScope.launch {
            runCatching { studentRepository.practiceProgress() }
                .onSuccess { progress -> _uiState.update { it.copy(progress = progress) } }
        }
        refreshWeek()
    }

    private fun refreshWeek() {
        if (!_uiState.value.signedIn) return
        viewModelScope.launch {
            runCatching { studentRepository.practiceWeek() }
                .onSuccess { week -> _uiState.update { it.copy(week = week) } }
        }
    }

    /** The card answers at once, and goes back if the server didn't take it. */
    fun setWeekGoal(goal: Int, reminders: Boolean) {
        val before = _uiState.value.week ?: return
        _uiState.update { it.copy(week = before.copy(goal = goal, reminders = reminders)) }
        viewModelScope.launch {
            runCatching { studentRepository.setPracticeWeek(goal, reminders) }
                .onSuccess { week -> _uiState.update { it.copy(week = week) } }
                .onFailure { error ->
                    _uiState.update { it.copy(week = before, message = error.message ?: "هدف هفته ذخیره نشد.") }
                }
        }
    }

    fun loadJournal() {
        _uiState.update { it.copy(journalFailed = false) }
        viewModelScope.launch {
            runCatching { studentRepository.practiceJournal() }
                .onSuccess { journal -> _uiState.update { it.copy(journal = journal) } }
                .onFailure { _uiState.update { it.copy(journalFailed = true) } }
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
    /** Whether a signed-in account is using the app; without one there are no lessons of one's own. */
    val signedIn: Boolean = false,
    /** The account the catalog was fetched for. */
    val catalogOwnerId: String? = null,
    /** Followed teacher ids. */
    val followedIds: Set<String> = emptySet(),
    /** Counted on the server. */
    val progress: PracticeProgress = PracticeProgress(),
    /** This week's practice days; null until known, and for someone not signed in. */
    val week: PracticeWeek? = null,
    /** Each section sung, first take against best; null until loaded. */
    val journal: List<JournalSection>? = null,
    val journalFailed: Boolean = false
) {
    val lastLessonId: String? get() = progress.lastLessonId
    val lastSectionId: String? get() = progress.lastSectionId

    /** What the most recent take scored. */
    val lastScore: Int? get() = progress.lastScore

    val teachers: List<Teacher>
        get() = catalog.teachers

    val hasContent: Boolean
        get() = catalog.lessons.isNotEmpty()

    val followedTeachers: List<Teacher>
        get() = teachers.filter { it.id in followedIds }

    /** What Home shows: lessons the student has, or has sung in (a paid lesson's free first section counts). */
    val myLessons: List<Lesson>
        get() = if (!signedIn) emptyList() else catalog.lessons.filter {
            it.isOwned == true || practicedSections(it) > 0
        }

    /** Every lesson on offer, the followed teachers' first. */
    val exploreLessons: List<Lesson>
        get() = catalog.lessons.filter { it.published }.sortedBy { it.teacherId !in followedIds }

    fun isFollowed(teacherId: String): Boolean = teacherId in followedIds

    fun teacher(teacherId: String): Teacher? = teachers.firstOrNull { it.id == teacherId }

    fun lessonsOf(teacherId: String): List<Lesson> = catalog.lessons.filter { it.teacherId == teacherId }

    fun lesson(lessonId: String): Lesson? = catalog.lessons.firstOrNull { it.id == lessonId }

    fun bestScore(lessonId: String, sectionId: String): Int? =
        progress.bestScores[PracticeProgress.key(lessonId, sectionId)]

    fun level(lessonId: String, sectionId: String): SectionLevel = PracticeLevels.level(
        best = bestScore(lessonId, sectionId),
        stamped = PracticeProgress.key(lessonId, sectionId) in progress.stamped
    )

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

    /** The lesson of the most recent take, when the student still has it open. */
    val lastPracticedLesson: Lesson?
        get() = lastLessonId?.let(::lesson)?.takeIf { it.isOpen && it.availableSections > 0 }

    /** The section to sing next in [lesson]: the first ready one not yet practiced; null once all are. */
    fun nextSection(lesson: Lesson): LessonSection? =
        lesson.sections.firstOrNull { it.isAvailable && bestScore(lesson.id, it.id) == null }

    /** The ready section after [sectionId] in [lesson], for going on straight from a practice. */
    fun sectionAfter(lesson: Lesson, sectionId: String): LessonSection? =
        lesson.sections.dropWhile { it.id != sectionId }.drop(1).firstOrNull { it.isAvailable }

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
