package app.melova.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.data.model.Review
import app.melova.data.model.StudentAccount
import app.melova.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The student's account and the teacher's replies. Activity-scoped: the account is created
 * here on the first launch, and [StudentUiState.accountRevision] changes when it is, so the
 * catalog can reload with the lessons this student owns.
 */
class StudentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StudentRepository(application)
    private val player = ReferenceAudioPlayer(application)

    private val _uiState = MutableStateFlow(StudentUiState(studentId = repository.studentId))
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Account and replies; creates the account the first time. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val created = repository.ensureAccount()
                Triple(created, repository.account(), repository.reviews())
            }.onSuccess { (created, account, reviews) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        studentId = account.id,
                        account = account,
                        reviews = reviews,
                        accountRevision = if (created) it.accountRevision + 1 else it.accountRevision
                    )
                }
            }.onFailure { error ->
                // Offline is fine: the app works without the account until the next refresh.
                _uiState.update { it.copy(isLoading = false, loadError = error.message) }
            }
        }
    }

    fun setName(name: String) {
        viewModelScope.launch {
            runCatching { repository.setName(name) }
                .onSuccess { account -> _uiState.update { it.copy(account = account) } }
                .onFailure { error -> _uiState.update { it.copy(message = error.message ?: "خطا") } }
        }
    }

    fun toggleReply(review: Review) {
        if (_uiState.value.playingReviewId == review.id) {
            player.pause()
            _uiState.update { it.copy(playingReviewId = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loadingReviewId = review.id) }
            runCatching { repository.replyAudio(review) }
                .onSuccess { file ->
                    player.stop()
                    player.prepare(file, onFinished = { _uiState.update { it.copy(playingReviewId = null) } })
                    player.play()
                    _uiState.update { it.copy(loadingReviewId = null, playingReviewId = review.id) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(loadingReviewId = null, message = error.message ?: "پخش ممکن نشد.") }
                }
        }
    }

    fun stopReply() {
        player.pause()
        _uiState.update { it.copy(playingReviewId = null) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}

data class StudentUiState(
    /** Shown to the student so the teacher can find them when they pay directly. */
    val studentId: String? = null,
    val account: StudentAccount? = null,
    val reviews: List<Review> = emptyList(),
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val playingReviewId: String? = null,
    val loadingReviewId: String? = null,
    val message: String? = null,
    val accountRevision: Int = 0
) {
    /** Answers the student hasn't necessarily seen: the newest few answered reviews. */
    val answeredCount: Int
        get() = reviews.count { it.status != app.melova.data.model.ReviewStatus.OPEN }
}
