package app.melova.presentation

import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import androidx.lifecycle.viewModelScope
import app.melova.data.model.LessonTier
import app.melova.data.model.LoginMethods
import app.melova.data.model.StudentAccount
import app.melova.data.repository.StudentRepository
import app.melova.platform.LocalFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The student's account. Activity-scoped: the account is created
 * here on the first launch, and [StudentUiState.accountRevision] changes when it is or when
 * signing in or out swaps it, so the catalog can reload with the lessons this student owns.
 */
class StudentViewModel : ViewModel() {
    private val repository = StudentRepository(appContext)

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    /** The version of the photo in the state, so each one is fetched once. */
    private var shownPhotoVersion: Long? = null

    init {
        refresh()
    }

    /** Creates the account the first time. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (_uiState.value.loginMethods == null) {
                val methods = runCatching { repository.loginMethods() }.getOrNull()
                _uiState.update { it.copy(loginMethods = methods) }
            }
            runCatching {
                val created = repository.ensureAccount()
                created to repository.account()
            }.onSuccess { (created, account) ->
                loadPhoto(account)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        account = account,
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

    /**
     * Pays for [tier] of a lesson. With a provider that finishes on its own page, the page's
     * address lands in [StudentUiState.paymentUrl] and the lesson opens once it says paid.
     */
    fun buyLesson(lessonId: String, tier: LessonTier) {
        val done = if (tier == LessonTier.WITH_TEACHER) "«درس با استاد» مال شماست." else "این درس مال شماست."
        changeLessons(done) {
            val checkout = repository.checkout(lessonId, tier)
            if (!checkout.paid) _uiState.update { it.copy(paymentUrl = checkout.redirectUrl) }
            checkout.account
        }
    }

    fun consumePaymentUrl() {
        _uiState.update { it.copy(paymentUrl = null) }
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
                        message = if (it.paymentUrl != null) null else done,
                        accountRevision = it.accountRevision + 1
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isChangingLessons = false, message = error.message ?: "پرداخت انجام نشد. دوباره امتحان کنید.") }
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
        viewModelScope.launch {
            // Offline the new guest account waits for the next refresh.
            runCatching { repository.signOut() }
            shownPhotoVersion = null
            _uiState.update {
                it.copy(
                    account = null,
                    photo = null,
                    accountRevision = it.accountRevision + 1
                )
            }
            refresh()
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

}

data class StudentUiState(
    val account: StudentAccount? = null,
    /** The student's photo as an encoded image. */
    val photo: ByteArray? = null,
    /** Null until the server has said, or when it can't. */
    val loginMethods: LoginMethods? = null,
    val isLoading: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isChangingLessons: Boolean = false,
    val loadError: String? = null,
    val message: String? = null,
    /** The payment provider's page, to open; null once opened or when payment needs none. */
    val paymentUrl: String? = null,
    val accountRevision: Int = 0
) {
    /** Buying and sending to a teacher take a real account, once the server offers a way in. */
    val needsLogin: Boolean
        get() = account?.signedIn == false && loginMethods?.any == true

    /** Teachers know a student by name, so whoever signs in without one gives it before going on. */
    val needsName: Boolean
        get() = account?.needsName == true

    val loginGate: LoginGate
        get() = when {
            needsLogin -> LoginGate.SIGN_IN
            account == null || needsName -> LoginGate.WAIT
            else -> LoginGate.OPEN
        }
}

/** How far someone is from what takes an account: every page that sends them to login asks this. */
enum class LoginGate {
    /** Not signed in, and the server offers a way in. */
    SIGN_IN,

    /** Past the login page but not done: the name page is up, or the account is on its way. */
    WAIT,
    OPEN
}
