package app.melova.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.melova.data.model.StudentAccount
import app.melova.data.repository.LoginChannel
import app.melova.data.repository.StudentRepository
import app.melova.platform.GoogleSignInResult
import app.melova.platform.appContext
import app.melova.platform.digitsOnly
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val LOGIN_CODE_LENGTH = 6

/** The login screen: a phone or email and the code sent to it, or Google. */
class AuthViewModel : ViewModel() {
    private val repository = StudentRepository(appContext)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdown: Job? = null

    fun setChannel(channel: LoginChannel) {
        _uiState.update { if (it.channel == channel) it else it.copy(channel = channel, address = "", error = null) }
    }

    fun setAddress(address: String) {
        _uiState.update { it.copy(address = address.take(MAX_ADDRESS_LENGTH), error = null) }
    }

    fun requestCode() {
        val state = _uiState.value
        if (state.address.isBlank()) return
        attempt {
            val sent = repository.requestLoginCode(state.channel, state.address)
            _uiState.update { it.copy(sentTo = sent.sentTo, code = "") }
            startCountdown(sent.resendAfterSeconds)
        }
    }

    /** For the server's mock login, which has no code to wait for. */
    fun signInWithoutCode() {
        val state = _uiState.value
        if (state.address.isBlank()) return
        attempt {
            val account = repository.verifyLoginCode(state.channel, state.address, code = "")
            _uiState.update { it.copy(signedIn = account) }
        }
    }

    /** Accepts Persian digits and a pasted code, showing them as typed; the last digit submits it. */
    fun setCode(text: String) {
        val code = text.filter { digitsOnly(it.toString()).isNotEmpty() }.take(LOGIN_CODE_LENGTH)
        _uiState.update { it.copy(code = code, error = null) }
        if (code.length == LOGIN_CODE_LENGTH) verifyCode(digitsOnly(code))
    }

    private fun verifyCode(code: String) {
        val state = _uiState.value
        attempt(onFailure = { _uiState.update { it.copy(code = "") } }) {
            val account = repository.verifyLoginCode(state.channel, state.address, code)
            _uiState.update { it.copy(signedIn = account) }
        }
    }

    /** Back from the code to the phone or email, to fix a typo. */
    fun backToAddress() {
        countdown?.cancel()
        _uiState.update { it.copy(sentTo = null, code = "", error = null, resendInSeconds = 0) }
    }

    fun onGoogleResult(result: GoogleSignInResult) {
        when (result) {
            GoogleSignInResult.Cancelled -> Unit
            is GoogleSignInResult.Failed -> _uiState.update { it.copy(error = result.message) }
            is GoogleSignInResult.Token -> attempt {
                val account = repository.signInWithGoogle(result.idToken)
                _uiState.update { it.copy(signedIn = account) }
            }
        }
    }

    private fun attempt(onFailure: () -> Unit = {}, block: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            runCatching { block() }.onFailure { error ->
                onFailure()
                _uiState.update { it.copy(error = error.message ?: "ورود ممکن نشد.") }
            }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (left in seconds downTo 1) {
                _uiState.update { it.copy(resendInSeconds = left) }
                delay(1000)
            }
            _uiState.update { it.copy(resendInSeconds = 0) }
        }
    }

    private companion object {
        const val MAX_ADDRESS_LENGTH = 254
    }
}

data class AuthUiState(
    val channel: LoginChannel = LoginChannel.PHONE,
    val address: String = "",
    /** Where the code went; set once it has, which moves the screen to the code step. */
    val sentTo: String? = null,
    val code: String = "",
    val resendInSeconds: Int = 0,
    val isBusy: Boolean = false,
    val error: String? = null,
    val signedIn: StudentAccount? = null
)
