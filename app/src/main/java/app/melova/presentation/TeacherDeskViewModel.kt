package app.melova.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.data.model.Review
import app.melova.data.model.ReviewStatus
import app.melova.data.model.StudentCard
import app.melova.data.model.TeacherReport
import app.melova.data.repository.TeacherDeskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The teacher's desk: takes students sent, the students who pay directly, and whether the
 * hours are worth it. Activity-scoped so the queue and a review share one list.
 */
class TeacherDeskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TeacherDeskRepository(application)
    private val player = ReferenceAudioPlayer(application)

    private val _uiState = MutableStateFlow(
        TeacherDeskUiState(privateHourlyRateToman = repository.privateHourlyRateToman)
    )
    val uiState: StateFlow<TeacherDeskUiState> = _uiState.asStateFlow()

    fun loadReviews(filter: ReviewStatus? = _uiState.value.filter) {
        _uiState.update { it.copy(filter = filter, isLoadingReviews = true) }
        viewModelScope.launch {
            runCatching { repository.reviews(filter) }
                .onSuccess { reviews -> _uiState.update { it.copy(reviews = reviews, isLoadingReviews = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingReviews = false, message = error.message ?: "خطا") }
                }
        }
    }

    /** Opening a review starts its clock on the server; the detail adds the score summary. */
    fun openReview(reviewId: String) {
        stopTake()
        _uiState.update { it.copy(current = it.reviews.firstOrNull { r -> r.id == reviewId }, isLoadingReview = true) }
        viewModelScope.launch {
            runCatching { repository.review(reviewId) }
                .onSuccess { review -> _uiState.update { it.copy(current = review, isLoadingReview = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingReview = false, message = error.message ?: "خطا") }
                }
        }
    }

    fun toggleTake() {
        val review = _uiState.value.current ?: return
        if (_uiState.value.isPlayingTake) {
            player.pause()
            _uiState.update { it.copy(isPlayingTake = false) }
            return
        }
        if (player.canResume) {
            player.play()
            _uiState.update { it.copy(isPlayingTake = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTake = true) }
            runCatching { repository.reviewAudio(review.id) }
                .onSuccess { file ->
                    player.stop()
                    player.prepare(file, onFinished = { _uiState.update { it.copy(isPlayingTake = false) } })
                    player.play()
                    _uiState.update { it.copy(isLoadingTake = false, isPlayingTake = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingTake = false, message = error.message ?: "پخش ممکن نشد.") }
                }
        }
    }

    fun stopTake() {
        player.stop()
        _uiState.update { it.copy(isPlayingTake = false) }
    }

    /** [minutes] null lets the server count from when the review was opened. */
    fun reply(text: String, voice: Uri?, minutes: Double?, onDone: () -> Unit) {
        val review = _uiState.value.current ?: return
        stopTake()
        run("در حال ارسال پاسخ…", onDone) {
            val answered = repository.reply(review.id, text, voice, minutes)
            _uiState.update { state ->
                state.copy(
                    current = answered,
                    reviews = state.reviews.filterNot { it.id == answered.id && state.filter == ReviewStatus.OPEN }
                        .map { if (it.id == answered.id) answered else it },
                    message = "پاسخ برای هنرجو فرستاده شد."
                )
            }
        }
    }

    fun decline(text: String, onDone: () -> Unit) {
        val review = _uiState.value.current ?: return
        stopTake()
        run("در حال بستن درخواست…", onDone) {
            val closed = repository.decline(review.id, text)
            _uiState.update { state ->
                state.copy(
                    current = closed,
                    reviews = state.reviews.filterNot { it.id == closed.id && state.filter == ReviewStatus.OPEN }
                        .map { if (it.id == closed.id) closed else it },
                    message = "بسته شد و اعتبار هنرجو برگشت."
                )
            }
        }
    }

    fun loadStudents() {
        _uiState.update { it.copy(isLoadingStudents = true) }
        viewModelScope.launch {
            runCatching { repository.students() }
                .onSuccess { students -> _uiState.update { it.copy(students = students, isLoadingStudents = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingStudents = false, message = error.message ?: "خطا") }
                }
        }
    }

    /** A card-to-card payment: opens [lessonId] and/or adds [reviews] for the student. */
    fun recordPayment(studentId: String, lessonId: String?, reviews: Int?, priceToman: Long, onDone: () -> Unit) =
        run("در حال ثبت پرداخت…", onDone) {
            val updated = repository.recordPayment(studentId, lessonId, reviews, priceToman)
            _uiState.update { state ->
                state.copy(
                    students = state.students.map { if (it.id == updated.id) updated else it },
                    message = "پرداخت ثبت شد."
                )
            }
        }

    fun loadReport() {
        _uiState.update { it.copy(isLoadingReport = true) }
        viewModelScope.launch {
            runCatching { repository.report() }
                .onSuccess { report -> _uiState.update { it.copy(report = report, isLoadingReport = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingReport = false, message = error.message ?: "خطا") }
                }
        }
    }

    fun setPrivateHourlyRate(toman: Long?) {
        repository.privateHourlyRateToman = toman
        _uiState.update { it.copy(privateHourlyRateToman = repository.privateHourlyRateToman) }
        loadReport()
    }

    /** Time spent outside reviews (prep, support, admin), so the report counts all of it. */
    fun logTime(minutes: Double, kind: String, note: String, onDone: () -> Unit) =
        run("در حال ثبت زمان…", onDone) {
            repository.logTime(minutes, kind, note)
            _uiState.update { it.copy(message = "زمان ثبت شد.") }
            loadReport()
        }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun run(busyLabel: String, onDone: () -> Unit, action: suspend () -> Unit) {
        if (_uiState.value.busyLabel != null) return
        _uiState.update { it.copy(busyLabel = busyLabel) }
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess {
                    _uiState.update { it.copy(busyLabel = null) }
                    onDone()
                }
                .onFailure { error -> _uiState.update { it.copy(busyLabel = null, message = error.message ?: "خطا") } }
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}

data class TeacherDeskUiState(
    /** Null for every review. */
    val filter: ReviewStatus? = ReviewStatus.OPEN,
    val reviews: List<Review> = emptyList(),
    val isLoadingReviews: Boolean = false,
    val current: Review? = null,
    val isLoadingReview: Boolean = false,
    val isPlayingTake: Boolean = false,
    val isLoadingTake: Boolean = false,
    val students: List<StudentCard> = emptyList(),
    val isLoadingStudents: Boolean = false,
    val report: TeacherReport? = null,
    val isLoadingReport: Boolean = false,
    val privateHourlyRateToman: Long? = null,
    val busyLabel: String? = null,
    val message: String? = null
)
