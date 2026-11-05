package app.melova.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.melova.audio.playback.ReferenceAudioPlayer
import app.melova.data.model.Chat
import app.melova.data.model.ChatMessage
import app.melova.data.model.ChatMessageKind
import app.melova.data.model.ChatThread
import app.melova.data.model.ChatUpdate
import app.melova.data.repository.ChatRepository
import app.melova.platform.ChatAlerts
import app.melova.platform.KeyValueStore
import app.melova.platform.LocalFile
import app.melova.platform.NotificationState
import app.melova.platform.appContext
import app.melova.platform.currentTimeMillis
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
    private var lastLocalId = 0L

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
                else ChatThread(update.chat, it.messages.merge(update))
            }
            state.copy(chats = chats, unread = chats.sumOf { it.unread }, thread = thread)
        }
        // A voice someone took back can't be the one in the player, or the one being answered.
        if (_uiState.value.activeVoiceId in update.removed) stopVoice()
        if (_uiState.value.replyToId in update.removed) cancelReply()
    }

    /** What the person has, with what changed written over it and what was taken back gone. */
    private fun List<ChatMessage>.merge(update: ChatUpdate): List<ChatMessage> =
        (this + update.messages)
            .filterNot { it.id in update.removed }
            .associateBy { it.id }
            .values
            .sortedBy { it.id }

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
        _uiState.update { it.copy(openChatId = null, threadError = null, replyToId = null) }
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
        val here = thread.messages
            .filter { it.kind == ChatMessageKind.VOICE && repository.hasVoice(it) }
            .map { it.id }
            .toSet()
        _uiState.update { state ->
            val shown = state.thread?.takeIf { it.chat.id == thread.chat.id }?.messages?.lastOrNull()?.id ?: 0L
            val isCurrent = (thread.messages.lastOrNull()?.id ?: 0L) >= shown
            if (state.openChatId == thread.chat.id && isCurrent) {
                state.copy(thread = thread, threadError = null, readyVoiceIds = state.readyVoiceIds + here)
            } else {
                state
            }
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

    /**
     * A message goes into the thread the moment it is written, as its own pending bubble, and
     * the composer is free again. The bubble carries how much of it has gone, and stays with a
     * way to send it again if it doesn't arrive.
     */
    fun sendText(text: String) {
        val chatId = _uiState.value.openChatId ?: return
        if (text.isBlank()) return
        enqueue(
            OutgoingMessage(
                localId = nextLocalId(),
                chatId = chatId,
                kind = ChatMessageKind.TEXT,
                text = text.trim(),
                replyTo = takeReply()
            )
        )
    }

    /** The recording is kept here first, so the recorder is free the moment the bubble appears. */
    fun sendVoice(voice: LocalFile, durationMs: Long, peaks: List<Float>) {
        val chatId = _uiState.value.openChatId ?: return
        val localId = nextLocalId()
        enqueue(
            OutgoingMessage(
                localId = localId,
                chatId = chatId,
                kind = ChatMessageKind.VOICE,
                voice = runCatching { repository.keepForSending(localId, voice) }.getOrElse { voice },
                durationMs = durationMs,
                peaks = peaks,
                replyTo = takeReply()
            )
        )
    }

    /** The message the next one answers, chosen by holding it. */
    fun startReply(messageId: Long) {
        _uiState.update { it.copy(replyToId = messageId) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyToId = null) }
    }

    /** Who is being answered, cleared as the answer goes: the next message is its own. */
    private fun takeReply(): Long? = _uiState.value.replyToId.also { cancelReply() }

    /** One more go at a message that didn't make it. */
    fun retrySend(localId: Long) {
        val pending = _uiState.value.outgoing.firstOrNull { it.localId == localId } ?: return
        if (!pending.hasFailed) return
        _uiState.update { it.withOutgoing(localId) { message -> message.copy(hasFailed = false, progress = 0f) } }
        deliver(pending.copy(hasFailed = false, progress = 0f))
    }

    /** Drops a message that didn't make it, without sending it. */
    fun discardSend(localId: Long) {
        repository.dropOutgoing(_uiState.value.outgoing.firstOrNull { it.localId == localId }?.voice)
        _uiState.update { state -> state.copy(outgoing = state.outgoing.filterNot { it.localId == localId }) }
    }

    private fun enqueue(message: OutgoingMessage) {
        _uiState.update { it.copy(outgoing = it.outgoing + message) }
        deliver(message)
    }

    private fun deliver(message: OutgoingMessage) {
        viewModelScope.launch {
            runCatching {
                when (message.kind) {
                    ChatMessageKind.VOICE -> repository.sendVoice(
                        message.chatId,
                        requireNotNull(message.voice),
                        message.durationMs,
                        message.replyTo
                    ) { sent -> onSendProgress(message.localId, sent) }
                    else -> repository.sendText(message.chatId, message.text, message.replyTo)
                }
            }
                .onSuccess { update ->
                    val sent = update.messages.lastOrNull { it.kind == ChatMessageKind.VOICE }
                    if (message.voice != null && sent != null) {
                        runCatching { repository.keepSentVoice(sent, message.voice) }
                            .onSuccess { _uiState.update { it.copy(readyVoiceIds = it.readyVoiceIds + sent.id) } }
                    }
                    repository.dropOutgoing(message.voice)
                    _uiState.update { state ->
                        state.copy(outgoing = state.outgoing.filterNot { it.localId == message.localId })
                    }
                    applyUpdate(update)
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.withOutgoing(message.localId) { it.copy(hasFailed = true) }
                            .copy(message = error.message ?: "پیام ارسال نشد.")
                    }
                }
        }
    }

    private fun onSendProgress(localId: Long, sent: Float) {
        _uiState.update { state -> state.withOutgoing(localId) { it.copy(progress = sent) } }
    }

    /** The sender rewrites their own words; both sides then see the new ones. */
    fun editText(messageId: Long, text: String) {
        val chatId = _uiState.value.openChatId ?: return
        if (text.isBlank() || messageId in _uiState.value.busyMessageIds) return
        changeMessage(messageId) { repository.editText(chatId, messageId, text) }
    }

    /** The sender takes their own message back, for both sides. */
    fun deleteMessage(messageId: Long) {
        val chatId = _uiState.value.openChatId ?: return
        if (messageId in _uiState.value.busyMessageIds) return
        changeMessage(messageId) { repository.deleteMessage(chatId, messageId) }
    }

    private fun changeMessage(messageId: Long, action: suspend () -> ChatUpdate) {
        _uiState.update { it.copy(busyMessageIds = it.busyMessageIds + messageId) }
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess(::applyUpdate)
                .onFailure { error -> _uiState.update { it.copy(message = error.message ?: "این پیام تغییر نکرد.") } }
            _uiState.update { it.copy(busyMessageIds = it.busyMessageIds - messageId) }
        }
    }

    private fun nextLocalId(): Long = ++lastLocalId

    fun toggleVoice(message: ChatMessage) {
        val state = _uiState.value
        if (state.activeVoiceId == message.id) {
            // Track the wanted state ourselves: ExoPlayer only reports playing once it has buffered.
            if (state.isVoicePlaying) player.pause() else player.play()
            _uiState.update { it.copy(isVoicePlaying = !state.isVoicePlaying) }
            return
        }
        openVoice(message, fromFraction = 0f)
    }

    /** Loads a voice if it isn't here yet, then plays it from where the finger left it. */
    private fun openVoice(message: ChatMessage, fromFraction: Float) {
        val chatId = _uiState.value.openChatId ?: return
        stopVoice()
        val isHere = repository.hasVoice(message)
        _uiState.update {
            it.copy(activeVoiceId = message.id, isVoiceLoading = true, voiceArrived = if (isHere) 1f else 0f)
        }
        viewModelScope.launch {
            runCatching { repository.voice(chatId, message) { arrived -> onVoiceArriving(message.id, arrived) } }
                .onSuccess { file ->
                    if (_uiState.value.activeVoiceId != message.id) return@onSuccess
                    _uiState.update { it.copy(readyVoiceIds = it.readyVoiceIds + message.id) }
                    playVoice(file, message.durationMs ?: 0L, fromFraction)
                }
                .onFailure { error ->
                    stopVoice()
                    _uiState.update { it.copy(message = error.message ?: "پخش ممکن نشد.") }
                }
        }
    }

    private fun onVoiceArriving(messageId: Long, arrived: Float) {
        _uiState.update { if (it.activeVoiceId == messageId) it.copy(voiceArrived = arrived) else it }
    }

    /** The finger is on the wave: the playhead follows it and the clock stops chasing it. */
    fun scrubVoice(message: ChatMessage, fraction: Float) {
        val state = _uiState.value
        if (state.activeVoiceId != message.id) {
            openVoice(message, fromFraction = fraction)
            return
        }
        val totalMs = state.voiceDurationMs.takeIf { it > 0L } ?: message.durationMs ?: 0L
        _uiState.update { it.copy(isScrubbing = true, voicePositionMs = (totalMs * fraction).toLong()) }
    }

    /** The finger is off it again, and the sound goes on from where it was left. */
    fun seekVoice(fraction: Float) {
        val state = _uiState.value
        if (state.activeVoiceId == null) return
        val targetMs = (state.voiceDurationMs * fraction).toLong()
        player.seekTo(targetMs)
        _uiState.update { it.copy(isScrubbing = false, voicePositionMs = targetMs) }
    }

    private fun playVoice(file: LocalFile, knownMs: Long, fromFraction: Float) {
        player.prepare(file, onFinished = ::stopVoice)
        if (fromFraction > 0f) player.seekTo((knownMs * fromFraction).toLong())
        player.play()
        _uiState.update { it.copy(isVoiceLoading = false, isVoicePlaying = true, voiceArrived = 1f) }
        voiceJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update {
                    // While a finger holds the playhead, it says where the sound is, not the player.
                    val position = if (it.isScrubbing) it.voicePositionMs else player.currentPositionMs()
                    it.copy(voicePositionMs = position, voiceDurationMs = player.durationMs())
                }
                delay(VOICE_TICK_MS)
            }
        }
    }

    fun stopVoice() {
        voiceJob?.cancel()
        player.stop()
        _uiState.update {
            it.copy(
                activeVoiceId = null,
                isVoiceLoading = false,
                isVoicePlaying = false,
                isScrubbing = false,
                voicePositionMs = 0L,
                voiceDurationMs = 0L,
                voiceArrived = 0f
            )
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

/**
 * A message written here that the server hasn't taken yet. It sits at the end of the thread as
 * its own bubble, with how much of it has gone, until it arrives or the person gives up on it.
 */
data class OutgoingMessage(
    val localId: Long,
    val chatId: String,
    val kind: ChatMessageKind,
    val text: String = "",
    val voice: LocalFile? = null,
    val durationMs: Long = 0L,
    /** The wave drawn on the bubble while it goes, so it looks like the one that will replace it. */
    val peaks: List<Float> = emptyList(),
    /** The message in the thread this one answers, when it answers one. */
    val replyTo: Long? = null,
    /** When it was written, shown as its time until the server's own replaces it. */
    val createdAtMs: Long = currentTimeMillis(),
    /** How much of it has gone, from 0 to 1; a text message is there or not. */
    val progress: Float = 0f,
    val hasFailed: Boolean = false
)

data class ChatUiState(
    val chats: List<Chat> = emptyList(),
    val unread: Int = 0,
    val hasLoaded: Boolean = false,
    val listError: String? = null,
    val openChatId: String? = null,
    val thread: ChatThread? = null,
    val threadError: String? = null,
    val isStartingChat: Boolean = false,
    /** Written here and not yet taken by the server, oldest first. */
    val outgoing: List<OutgoingMessage> = emptyList(),
    /** Messages being rewritten or taken back right now. */
    val busyMessageIds: Set<Long> = emptySet(),
    /** The voice message loaded in the player, playing or paused. */
    val activeVoiceId: Long? = null,
    val isVoiceLoading: Boolean = false,
    val isVoicePlaying: Boolean = false,
    /** A finger is holding the playhead, so nothing else moves it. */
    val isScrubbing: Boolean = false,
    val voicePositionMs: Long = 0L,
    val voiceDurationMs: Long = 0L,
    /** How much of the voice being opened has arrived, from 0 to 1. */
    val voiceArrived: Float = 0f,
    /** Voice messages already here, which play without waiting for the network. */
    val readyVoiceIds: Set<Long> = emptySet(),
    val replyToId: Long? = null,
    val notifications: NotificationState = NotificationState.UNSUPPORTED,
    val message: String? = null
)

/** The same state with one pending message changed. */
private fun ChatUiState.withOutgoing(localId: Long, change: (OutgoingMessage) -> OutgoingMessage): ChatUiState =
    copy(outgoing = outgoing.map { if (it.localId == localId) change(it) else it })
