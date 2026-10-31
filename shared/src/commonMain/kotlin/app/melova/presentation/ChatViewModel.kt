package app.melova.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.data.model.Chat
import app.melova.data.model.ChatMessage
import app.melova.data.model.ChatThread
import app.melova.data.model.ChatUpdate
import app.melova.data.repository.ChatRepository
import app.melova.platform.ChatAlerts
import app.melova.platform.LocalFile
import app.melova.platform.KeyValueStore
import app.melova.platform.NotificationState
import app.melova.platform.appContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The person's chats and the one they have open. Activity-scoped, as Home shows the unread
 * count. What the other side does arrives over a socket, and the chats are read again each
 * time it connects, which covers whatever happened while it was down.
 */
class ChatViewModel : ViewModel() {
    private val repository = ChatRepository(appContext)
    private val player = ReferenceAudioPlayer(appContext)
    private val alertChoice = KeyValueStore(appContext, "chat_alert_choice")
    private var voiceJob: Job? = null
    private var reportedSeenId = 0L

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Keeps the live connection up until the caller is cancelled, as the app left the screen. */
    suspend fun stayConnected() {
        var retryMs = MIN_RETRY_MS
        while (true) {
            runCatching {
                repository.listen(
                    onReady = {
                        retryMs = MIN_RETRY_MS
                        catchUp()
                    },
                    onUpdate = ::applyUpdate
                )
            }
            currentCoroutineContext().ensureActive()
            // Reading here too keeps the chats moving on a network that won't carry a socket.
            catchUp()
            delay(retryMs)
            retryMs = (retryMs * 2).coerceAtMost(MAX_RETRY_MS)
        }
    }

    private fun catchUp() {
        refreshChats()
        _uiState.value.openChatId?.let(::refreshThread)
    }

    private fun applyUpdate(update: ChatUpdate) {
        _uiState.update { state ->
            val chats = (state.chats.filterNot { it.id == update.chat.id } + update.chat)
                .sortedByDescending { it.lastMessageAtMs }
            val thread = state.thread?.let {
                if (it.chat.id != update.chat.id) it
                else ChatThread(update.chat, (it.messages + update.messages).associateBy { m -> m.id }.values.sortedBy { m -> m.id })
            }
            state.copy(chats = chats, unread = chats.sumOf { it.unread }, thread = thread)
        }
    }

    /** A failed refresh keeps what is on screen until the next one. */
    fun refreshChats() {
        viewModelScope.launch {
            runCatching { repository.inbox() }
                .onSuccess { inbox ->
                    _uiState.update { it.copy(chats = inbox.chats, unread = inbox.unread, hasLoaded = true, listError = null) }
                }
                .onFailure { error -> _uiState.update { it.copy(listError = error.message ?: "گفتگوها باز نشد.") } }
        }
    }

    fun openThread(chatId: String) {
        _uiState.update {
            it.copy(openChatId = chatId, thread = it.thread?.takeIf { thread -> thread.chat.id == chatId }, threadError = null)
        }
        refreshThread(chatId)
    }

    /** The thread stays, to show at once when the person comes back from a result page. */
    fun closeThread() {
        stopVoice()
        _uiState.update { it.copy(openChatId = null, threadError = null) }
    }

    /** The chat with a teacher, opened from their page. */
    fun startChat(teacherId: String, onReady: (chatId: String) -> Unit) {
        if (_uiState.value.isStartingChat) return
        _uiState.update { it.copy(isStartingChat = true) }
        viewModelScope.launch {
            runCatching { repository.startChat(teacherId) }
                .onSuccess { chatId ->
                    _uiState.update { it.copy(isStartingChat = false) }
                    onReady(chatId)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isStartingChat = false, message = error.message ?: "گفتگو باز نشد.") }
                }
        }
    }

    private fun refreshThread(chatId: String) {
        viewModelScope.launch {
            runCatching { repository.thread(chatId) }
                .onSuccess(::showThread)
                .onFailure { error ->
                    _uiState.update { state ->
                        if (state.thread == null) state.copy(threadError = error.message ?: "گفتگو باز نشد.") else state
                    }
                }
        }
    }

    /** An answer for a chat the person left, or one older than the send it crossed, stays out. */
    private fun showThread(thread: ChatThread) {
        _uiState.update { state ->
            val shown = state.thread?.takeIf { it.chat.id == thread.chat.id }?.messages?.lastOrNull()?.id ?: 0L
            val isCurrent = (thread.messages.lastOrNull()?.id ?: 0L) >= shown
            if (state.openChatId == thread.chat.id && isCurrent) state.copy(thread = thread, threadError = null) else state
        }
    }

    /** The newest message is on screen, so the other side can be told it was seen. */
    fun markSeen() {
        val state = _uiState.value
        val thread = state.thread?.takeIf { it.chat.id == state.openChatId } ?: return
        val newest = thread.messages.lastOrNull()?.id ?: return
        if (thread.chat.unread == 0 || newest == reportedSeenId) return
        reportedSeenId = newest
        viewModelScope.launch {
            runCatching { repository.markRead(thread.chat.id, newest) }
                .onSuccess(::applyUpdate)
                .onFailure { reportedSeenId = 0L }
        }
    }

    fun sendText(text: String, onSent: () -> Unit) = send(onSent) { chatId -> repository.sendText(chatId, text) }

    fun sendVoice(voice: LocalFile, durationMs: Long, onSent: () -> Unit) =
        send(onSent) { chatId -> repository.sendVoice(chatId, voice, durationMs) }

    private fun send(onSent: () -> Unit, action: suspend (String) -> ChatUpdate) {
        val chatId = _uiState.value.openChatId ?: return
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            runCatching { action(chatId) }
                .onSuccess { update ->
                    _uiState.update { it.copy(isSending = false) }
                    applyUpdate(update)
                    onSent()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSending = false, message = error.message ?: "پیام ارسال نشد.") }
                }
        }
    }

    fun toggleVoice(message: ChatMessage) {
        val state = _uiState.value
        val chatId = state.openChatId ?: return
        if (state.activeVoiceId == message.id) {
            // Track the wanted state ourselves: ExoPlayer only reports playing once it has buffered.
            if (state.isVoicePlaying) player.pause() else player.play()
            _uiState.update { it.copy(isVoicePlaying = !state.isVoicePlaying) }
            return
        }
        stopVoice()
        _uiState.update { it.copy(activeVoiceId = message.id, isVoiceLoading = true) }
        viewModelScope.launch {
            runCatching { repository.voice(chatId, message) }
                .onSuccess { file -> if (_uiState.value.activeVoiceId == message.id) playVoice(file) }
                .onFailure { error ->
                    stopVoice()
                    _uiState.update { it.copy(message = error.message ?: "پخش ممکن نشد.") }
                }
        }
    }

    /** A tap on the wave of the voice in the player. */
    fun seekVoice(fraction: Float) {
        val targetMs = (_uiState.value.voiceDurationMs * fraction).toLong()
        player.seekTo(targetMs)
        _uiState.update { it.copy(voicePositionMs = targetMs) }
    }

    private fun playVoice(file: LocalFile) {
        player.prepare(file, onFinished = ::stopVoice)
        player.play()
        _uiState.update { it.copy(isVoiceLoading = false, isVoicePlaying = true) }
        voiceJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update {
                    it.copy(voicePositionMs = player.currentPositionMs(), voiceDurationMs = player.durationMs())
                }
                delay(VOICE_TICK_MS)
            }
        }
    }

    fun stopVoice() {
        voiceJob?.cancel()
        player.stop()
        _uiState.update {
            it.copy(activeVoiceId = null, isVoiceLoading = false, isVoicePlaying = false, voicePositionMs = 0L, voiceDurationMs = 0L)
        }
    }

    /** Sign-in and sign-out change whose chats these are. */
    fun clear() {
        stopVoice()
        _uiState.update { ChatUiState(notifications = it.notifications) }
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            val state = ChatAlerts.state()
            _uiState.update { it.copy(notifications = state) }
        }
    }

    /** Notifications start on, so the very first opening asks; a choice made after that stands. */
    fun askAtStart(ask: () -> Unit) {
        viewModelScope.launch {
            if (alertChoice.getInt(KEY_ASKED) != null || alertChoice.getInt(KEY_TURNED_OFF) != null) return@launch
            if (ChatAlerts.isAllowed() || ChatAlerts.state() != NotificationState.OFF) return@launch
            ChatAlerts.atFirstChance {
                alertChoice.putInt(KEY_ASKED, 1)
                ask()
            }
        }
    }

    fun enableNotifications() {
        alertChoice.remove(KEY_TURNED_OFF)
        viewModelScope.launch {
            runCatching { ChatAlerts.turnOn { repository.pushKey() }?.let { repository.subscribe(it) } }
                .onFailure { _uiState.update { it.copy(message = "اعلان روشن نشد. دوباره امتحان کنید.") } }
            refreshNotifications()
        }
    }

    fun disableNotifications() {
        alertChoice.putInt(KEY_TURNED_OFF, 1)
        viewModelScope.launch {
            runCatching { ChatAlerts.turnOff()?.let { repository.unsubscribe(it) } }
            refreshNotifications()
        }
    }

    fun onNotificationsRefused() {
        _uiState.update { it.copy(message = "اعلان ملووا مسدود است. از تنظیمات گوشی یا مرورگر آن را باز کنید.") }
        refreshNotifications()
    }

    /** A device with notifications on hears about whoever is signed in on it now. */
    fun syncNotifications() {
        viewModelScope.launch {
            val wanted = alertChoice.getInt(KEY_TURNED_OFF) == null && ChatAlerts.isAllowed()
            runCatching {
                val address = if (wanted) ChatAlerts.turnOn { repository.pushKey() } else ChatAlerts.address()
                address?.let { repository.subscribe(it) }
            }
            ChatAlerts.keepListening()
            refreshNotifications()
        }
    }

    /** Signing out: this device stops hearing about the account, and picks up again on the next sign-in. */
    fun forgetThisDevice() {
        ChatAlerts.stopListening()
        viewModelScope.launch { runCatching { ChatAlerts.address()?.let { repository.unsubscribe(it) } } }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    private companion object {
        const val VOICE_TICK_MS = 100L
        const val MIN_RETRY_MS = 2_000L
        const val MAX_RETRY_MS = 30_000L
        const val KEY_ASKED = "asked"
        const val KEY_TURNED_OFF = "turned_off"
    }
}

data class ChatUiState(
    val chats: List<Chat> = emptyList(),
    val unread: Int = 0,
    val hasLoaded: Boolean = false,
    val listError: String? = null,
    val openChatId: String? = null,
    val thread: ChatThread? = null,
    val threadError: String? = null,
    val isStartingChat: Boolean = false,
    val isSending: Boolean = false,
    /** The voice message loaded in the player, playing or paused. */
    val activeVoiceId: Long? = null,
    val isVoiceLoading: Boolean = false,
    val isVoicePlaying: Boolean = false,
    val voicePositionMs: Long = 0L,
    val voiceDurationMs: Long = 0L,
    val notifications: NotificationState = NotificationState.UNSUPPORTED,
    val message: String? = null
)
