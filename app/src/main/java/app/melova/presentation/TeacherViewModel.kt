package app.melova.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.melova.data.model.Course
import app.melova.data.model.Lesson
import app.melova.data.model.TeacherProfile
import app.melova.data.repository.CourseFields
import app.melova.data.repository.LessonFields
import app.melova.data.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Teacher mode: sign in with the teacher code and manage courses. Activity-scoped so the
 * panel, course and lesson editors share one profile. [TeacherUiState.revision] changes
 * whenever the catalog does, so the student side can reload.
 */
class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TeacherRepository(application)

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

    fun refresh() = run(busyLabel = null, changesCatalog = false) { repository.profile() }

    fun updateProfile(name: String, bio: String) =
        run("در حال ذخیره…") { repository.updateProfile(name, bio) }

    fun createCourse(fields: CourseFields) = run("در حال ساخت دوره…") { repository.createCourse(fields) }

    fun updateCourse(courseId: String, fields: CourseFields) =
        run("در حال ذخیره…") { repository.updateCourse(courseId, fields) }

    fun setCoursePublished(courseId: String, published: Boolean) =
        run(if (published) "در حال انتشار…" else "در حال برگرداندن به پیش‌نویس…") {
            repository.setCoursePublished(courseId, published)
        }

    fun deleteCourse(courseId: String) = run("در حال حذف دوره…") { repository.deleteCourse(courseId) }

    fun createLesson(courseId: String, fields: LessonFields) =
        run("در حال ساخت درس…") { repository.createLesson(courseId, fields) }

    fun updateLesson(lessonId: String, fields: LessonFields) =
        run("در حال ذخیره…") { repository.updateLesson(lessonId, fields) }

    fun deleteLesson(lessonId: String) = run("در حال حذف درس…") { repository.deleteLesson(lessonId) }

    fun uploadIntro(lessonId: String, audio: Uri) =
        run("در حال بارگذاری آموزش…") { repository.uploadIntro(lessonId, audio) }

    fun addSection(lessonId: String, title: String, description: String, audio: Uri) =
        run("در حال بارگذاری و آماده‌سازی بخش…") {
            repository.addSection(lessonId, title, description, audio)
        }

    fun deleteSection(lessonId: String, sectionId: String) =
        run("در حال حذف بخش…") { repository.deleteSection(lessonId, sectionId) }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun run(
        busyLabel: String?,
        changesCatalog: Boolean = true,
        action: suspend () -> TeacherProfile
    ) {
        if (_uiState.value.busyLabel != null) return
        _uiState.update { it.copy(busyLabel = busyLabel, isLoading = busyLabel == null) }
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            isSignedIn = true,
                            profile = profile,
                            busyLabel = null,
                            isLoading = false,
                            revision = if (changesCatalog) it.revision + 1 else it.revision
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSignedIn = repository.isSignedIn,
                            busyLabel = null,
                            isLoading = false,
                            message = error.message ?: "خطا"
                        )
                    }
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
    /** One-off error or notice for a snackbar. */
    val message: String? = null,
    val revision: Int = 0
) {
    fun course(courseId: String): Course? = profile?.courses?.firstOrNull { it.id == courseId }

    fun lesson(lessonId: String): Pair<Course, Lesson>? = profile?.courses?.firstNotNullOfOrNull { course ->
        course.lessons.firstOrNull { it.id == lessonId }?.let { course to it }
    }
}
