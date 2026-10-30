package app.melova.presentation

import androidx.lifecycle.ViewModel
import app.melova.platform.appContext
import androidx.lifecycle.viewModelScope
import app.melova.data.model.LessonPurchaseRequest
import app.melova.data.model.StudentCard
import app.melova.data.model.TeacherReport
import app.melova.data.repository.TeacherDeskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The teacher's desk: the students who pay directly, and whether the hours are worth it.
 * Activity-scoped so the panel and the students page share the waiting payments.
 */
class TeacherDeskViewModel : ViewModel() {
    private val repository = TeacherDeskRepository(appContext)

    private val _uiState = MutableStateFlow(
        TeacherDeskUiState(privateHourlyRateToman = repository.privateHourlyRateToman)
    )
    val uiState: StateFlow<TeacherDeskUiState> = _uiState.asStateFlow()

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

    /** Opens [lessonId] for a student who paid outside the app. */
    fun recordPayment(studentId: String, lessonId: String, priceToman: Long, onDone: () -> Unit) =
        run("در حال ثبت پرداخت…", onDone) {
            val updated = repository.recordPayment(studentId, lessonId, priceToman)
            _uiState.update { state ->
                state.copy(
                    students = state.students.map { if (it.id == updated.id) updated else it },
                    message = "درس برای هنرجو باز شد."
                )
            }
            loadPurchaseRequests()
        }

    fun loadPurchaseRequests() {
        viewModelScope.launch {
            runCatching { repository.purchaseRequests() }
                .onSuccess { requests -> _uiState.update { it.copy(purchaseRequests = requests) } }
        }
    }

    /** Confirming opens the lesson for the student; refusing tells them to check their payment. */
    fun decidePurchase(requestId: String, approve: Boolean) =
        run(if (approve) "در حال تأیید پرداخت…" else "در حال رد پرداخت…", onDone = ::loadStudents) {
            val waiting = repository.decidePurchase(requestId, approve)
            _uiState.update {
                it.copy(
                    purchaseRequests = waiting,
                    message = if (approve) "درس برای هنرجو باز شد." else "به هنرجو گفته شد پرداختش پیدا نشد."
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
}

data class TeacherDeskUiState(
    val students: List<StudentCard> = emptyList(),
    val isLoadingStudents: Boolean = false,
    val purchaseRequests: List<LessonPurchaseRequest> = emptyList(),
    val report: TeacherReport? = null,
    val isLoadingReport: Boolean = false,
    val privateHourlyRateToman: Long? = null,
    val busyLabel: String? = null,
    val message: String? = null
)
