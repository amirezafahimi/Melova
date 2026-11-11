package app.melova.presentation

import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import androidx.lifecycle.viewModelScope
import app.melova.data.model.TeacherEarnings
import app.melova.data.model.TeacherReport
import app.melova.data.repository.TeacherDeskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The teacher's desk: what their lessons sold and earned them, and whether the hours are worth it.
 * Activity-scoped so the panel and the earnings page share what was loaded.
 */
class TeacherDeskViewModel : ViewModel() {
    private val repository = TeacherDeskRepository(appContext)

    private val _uiState = MutableStateFlow(
        TeacherDeskUiState(privateHourlyRateCents = repository.privateHourlyRateCents)
    )
    val uiState: StateFlow<TeacherDeskUiState> = _uiState.asStateFlow()

    /** Another account is using the app: its sales and report are not this one's. */
    fun onAccountChanged() {
        _uiState.value = TeacherDeskUiState(privateHourlyRateCents = repository.privateHourlyRateCents)
    }

    fun loadEarnings() {
        _uiState.update { it.copy(isLoadingEarnings = true) }
        viewModelScope.launch {
            runCatching { repository.earnings() }
                .onSuccess { earnings -> _uiState.update { it.copy(earnings = earnings, isLoadingEarnings = false) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingEarnings = false, message = error.message ?: "فروش‌ها باز نشد.") }
                }
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

    fun setPrivateHourlyRate(cents: Long?) {
        repository.privateHourlyRateCents = cents
        _uiState.update { it.copy(privateHourlyRateCents = repository.privateHourlyRateCents) }
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
}

data class TeacherDeskUiState(
    val earnings: TeacherEarnings? = null,
    val isLoadingEarnings: Boolean = false,
    val report: TeacherReport? = null,
    val isLoadingReport: Boolean = false,
    val privateHourlyRateCents: Long? = null,
    val busyLabel: String? = null,
    val message: String? = null
)
