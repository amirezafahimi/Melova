package app.melova.presentation

import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import app.melova.platform.LocalFile
import androidx.lifecycle.viewModelScope
import app.melova.data.model.Lesson
import app.melova.data.model.TeacherProfile
import app.melova.data.repository.LessonFields
import app.melova.data.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Teacher mode: sign in with the teacher code and manage lessons. Activity-scoped so the
 * panel and the lesson editor share one profile. [TeacherUiState.revision] changes
 * whenever the catalog does, so the student side can reload.
 */
class TeacherViewModel : ViewModel() {
    private val repository = TeacherRepository(appContext)

    private val _uiState = MutableStateFlow(TeacherUiState(isSignedIn = repository.isSignedIn))
    val uiState: StateFlow<TeacherUiState> = _uiState.asStateFlow()

    init {
        if (repository.isSignedIn) refresh()
    }

    fun signIn(code: String) {
        if (code.isBlank()) return
        run(busyLabel = "در حال بررسی کد…") { repository.signIn(code) }
    }

    fun signOut() {
        repository.signOut()
        _uiState.update { TeacherUiState(revision = it.revision + 1) }
    }

    /** Signing in or out, or saying they teach, can open or close the panel. */
    fun onAccountChanged() {
        val isOpen = _uiState.value.isSignedIn
        when {
            repository.isSignedIn && !isOpen -> run(busyLabel = null) { repository.profile() }
            !repository.isSignedIn && isOpen -> signOut()
            // Straight from one teacher to another: the panel must show the new one's profile.
            repository.isSignedIn && isOpen -> refresh()
        }
    }

    fun refresh() = run(busyLabel = null, changesCatalog = false) { repository.profile() }

    fun updateProfile(name: String, bio: String) =
        run("در حال ذخیره…") { repository.updateProfile(name, bio) }

    fun setPhoto(photo: LocalFile) = run("در حال بارگذاری عکس…") { repository.setPhoto(photo) }

    fun removePhoto() = run("در حال حذف عکس…") { repository.removePhoto() }

    /** [onCreated] gets the new lesson's id, so the panel can open it right away. */
    fun createLesson(fields: LessonFields, onCreated: (String) -> Unit = {}) {
        val before = _uiState.value.profile?.lessons.orEmpty().map { it.id }.toSet()
        run("در حال ساخت درس…", onSuccess = {
            _uiState.value.profile?.lessons?.firstOrNull { it.id !in before }?.let { onCreated(it.id) }
        }) { repository.createLesson(fields) }
    }

    fun setLessonPublished(lessonId: String, published: Boolean) =
        run(if (published) "در حال انتشار…" else "در حال برگرداندن به پیش‌نویس…") {
            repository.setLessonPublished(lessonId, published)
        }

    fun updateLesson(lessonId: String, fields: LessonFields) =
        run("در حال ذخیره…") { repository.updateLesson(lessonId, fields) }

    /** A [priceCents] of 0 makes the lesson free; a [teacherPriceCents] of 0 sells only «درس». */
    fun setLessonSale(lessonId: String, priceCents: Long, teacherPriceCents: Long) =
        run("در حال ذخیره قیمت…") { repository.setLessonSale(lessonId, priceCents, teacherPriceCents) }

    fun deleteLesson(lessonId: String) = run("در حال حذف درس…") { repository.deleteLesson(lessonId) }

    fun uploadIntro(lessonId: String, audio: LocalFile, onSuccess: () -> Unit = {}) =
        run("در حال بارگذاری آموزش…", onSuccess = onSuccess) {
            repository.uploadIntro(lessonId, audio) { sent ->
                // Once it is all sent, the server still converts it before answering.
                _uiState.update {
                    it.copy(
                        uploadProgress = sent,
                        busyLabel = if (sent >= 1f) "در حال آماده‌سازی آموزش…" else it.busyLabel
                    )
                }
            }
        }

    /** [onFailure] gets the error instead of the snackbar, for a dialog that stays open. */
    fun addSection(
        lessonId: String,
        title: String,
        description: String,
        takeId: String,
        onSuccess: () -> Unit = {},
        onFailure: ((String) -> Unit)? = null
    ) = run(
        "در حال آماده‌سازی بخش…",
        onSuccess = onSuccess,
        onFailure = onFailure
    ) {
        repository.addSection(lessonId, title, description, takeId)
    }

    /** A null [takeId] keeps the take; a new one has its poem read, so it takes longer. */
    fun updateSection(
        lessonId: String,
        sectionId: String,
        title: String,
        description: String,
        takeId: String?,
        onSuccess: () -> Unit = {},
        onFailure: ((String) -> Unit)? = null
    ) = run(
        if (takeId == null) "در حال ذخیره…" else "در حال آماده‌سازی بخش…",
        onSuccess = onSuccess,
        onFailure = onFailure
    ) {
        repository.updateSection(lessonId, sectionId, title, description, takeId)
    }

    fun deleteSection(lessonId: String, sectionId: String, onSuccess: () -> Unit = {}) =
        run("در حال حذف بخش…", onSuccess = onSuccess) { repository.deleteSection(lessonId, sectionId) }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun run(
        busyLabel: String?,
        changesCatalog: Boolean = true,
        onSuccess: () -> Unit = {},
        onFailure: ((String) -> Unit)? = null,
        action: suspend () -> TeacherProfile
    ) {
        if (_uiState.value.busyLabel != null) return
        _uiState.update { it.copy(busyLabel = busyLabel, uploadProgress = null, isLoading = busyLabel == null) }
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isSignedIn = true,
                            profile = profile,
                            busyLabel = null,
                            uploadProgress = null,
                            isLoading = false,
                            revision = if (changesCatalog) it.revision + 1 else it.revision
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    val text = error.message ?: "خطا"
                    _uiState.update {
                        it.copy(
                            isSignedIn = repository.isSignedIn,
                            busyLabel = null,
                            uploadProgress = null,
                            isLoading = false,
                            message = if (onFailure == null) text else it.message
                        )
                    }
                    onFailure?.invoke(text)
                }
        }
    }
}

data class TeacherUiState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val profile: TeacherProfile? = null,
    /** Non-null while a change is being sent; shown as a blocking progress overlay. */
    val busyLabel: String? = null,
    /** How much of a file being sent has gone, 0 to 1; null when nothing is being sent. */
    val uploadProgress: Float? = null,
    /** One-off error or notice for a snackbar. */
    val message: String? = null,
    val revision: Int = 0
) {
    fun lesson(lessonId: String): Lesson? = profile?.lessons?.firstOrNull { it.id == lessonId }
}
