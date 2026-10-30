package app.melova.presentation

import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import androidx.lifecycle.viewModelScope
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.data.model.LoginMethods
import app.melova.data.model.Review
import app.melova.data.model.StudentAccount
import app.melova.data.repository.StudentRepository
import app.melova.platform.LocalFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The student's account and the teacher's replies. Activity-scoped: the account is created
 * here on the first launch, and [StudentUiState.accountRevision] changes when it is or when
 * signing in or out swaps it, so the catalog can reload with the lessons this student owns.
 */
class StudentViewModel : ViewModel() {
    private val repository = StudentRepository(appContext)
    private val player = ReferenceAudioPlayer(appContext)

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    /** The version of the photo in the state, so each one is fetched once. */
    private var shownPhotoVersion: Long? = null

    init {
        refresh()
    }

    /** Account and replies; creates the account the first time. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (_uiState.value.loginMethods == null) {
                val methods = runCatching { repository.loginMethods() }.getOrNull()
                _uiState.update { it.copy(loginMethods = methods) }
            }
            runCatching {
                val created = repository.ensureAccount()
                Triple(created, repository.account(), repository.reviews())
            }.onSuccess { (created, account, reviews) ->
                loadPhoto(account)
                _uiState.update {
                    it.copy(
                        isLoading = false,
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

    fun setName(name: String) = saveProfile { repository.setName(name) }

    fun setPhoto(photo: LocalFile) = saveProfile { repository.setPhoto(photo) }

    fun removePhoto() = saveProfile { repository.removePhoto() }

    fun sayITeach(code: String) = saveProfile { repository.sayITeach(code) }

    private fun saveProfile(save: suspend () -> StudentAccount) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true) }
            runCatching { save() }
                .onSuccess { account ->
                    loadPhoto(account)
                    _uiState.update { it.copy(account = account, isSavingProfile = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSavingProfile = false, message = error.message ?: "ذخیره نشد.") }
                }
        }
    }

    /** Adds a free lesson to the student's lessons. */
    fun addLesson(lessonId: String) = changeLessons("این درس به درس‌های شما اضافه شد.") {
        repository.claimFreeLesson(lessonId)
    }

    /** The student paid for a lesson by card, under the name [payer]. */
    fun buyLesson(lessonId: String, payer: String) =
        changeLessons("پرداخت ثبت شد. بعد از تأیید استاد، درس باز می‌شود.") {
            repository.requestLesson(lessonId, payer)
        }

    private fun changeLessons(done: String, change: suspend () -> StudentAccount) {
        if (_uiState.value.isChangingLessons) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingLessons = true) }
            runCatching {
                repository.ensureAccount()
                change()
            }.onSuccess { account ->
                _uiState.update {
                    it.copy(
                        account = account,
                        isChangingLessons = false,
                        message = done,
                        accountRevision = it.accountRevision + 1
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isChangingLessons = false, message = error.message ?: "انجام نشد.") }
            }
        }
    }

    /** Offline it stays as it was, and the next refresh tries again. */
    private suspend fun loadPhoto(account: StudentAccount) {
        if (account.photoVersion == shownPhotoVersion) return
        val photo = runCatching { repository.photo(account) }.getOrElse { return }
        shownPhotoVersion = account.photoVersion
        _uiState.update { it.copy(photo = photo) }
    }

    /** True once per install: a guest's first visit to the profile opens on sign-in. */
    fun takeLoginOffer(): Boolean = repository.takeLoginOffer()

    /** Sign-in gave this install another account, with its own lessons and replies. */
    fun onSignedIn(account: StudentAccount) {
        _uiState.update {
            it.copy(account = account, accountRevision = it.accountRevision + 1)
        }
        refresh()
    }

    fun signOut() {
        stopReply()
        viewModelScope.launch {
            // Offline the new guest account waits for the next refresh.
            runCatching { repository.signOut() }
            shownPhotoVersion = null
            _uiState.update {
                it.copy(
                    account = null,
                    photo = null,
                    reviews = emptyList(),
                    accountRevision = it.accountRevision + 1
                )
            }
            refresh()
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
    val account: StudentAccount? = null,
    /** The student's photo as an encoded image. */
    val photo: ByteArray? = null,
    /** Null until the server has said, or when it can't. */
    val loginMethods: LoginMethods? = null,
    val reviews: List<Review> = emptyList(),
    val isLoading: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isChangingLessons: Boolean = false,
    val loadError: String? = null,
    val playingReviewId: String? = null,
    val loadingReviewId: String? = null,
    val message: String? = null,
    val accountRevision: Int = 0
) {
    /** Answers the student hasn't necessarily seen: the newest few answered reviews. */
    val answeredCount: Int
        get() = reviews.count { it.status != app.melova.data.model.ReviewStatus.OPEN }

    /** Buying and sending to a teacher take a real account, once the server offers a way in. */
    val needsLogin: Boolean
        get() = account?.signedIn == false && loginMethods?.any == true
}
