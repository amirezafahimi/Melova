package app.melova.data.repository

import app.melova.data.local.TeacherSession
import app.melova.data.model.Chat
import app.melova.data.model.ChatInbox
import app.melova.data.model.ChatMessage
import app.melova.data.model.ChatMessageKind
import app.melova.data.model.ChatSide
import app.melova.data.model.ChatThread
import app.melova.data.model.ChatUpdate
import app.melova.data.model.PracticeResultLink
import app.melova.data.model.ReviewStatus
import app.melova.data.model.SharedTake
import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.api.ChatApi
import app.melova.data.remote.apiCall
import app.melova.data.remote.dto.ChatDto
import app.melova.data.remote.dto.ChatEventDto
import app.melova.data.remote.dto.ChatMessageDto
import app.melova.data.remote.dto.ChatThreadDto
import app.melova.data.remote.dto.PracticeResultLinkDto
import app.melova.data.remote.mapper.toDomain
import app.melova.data.remote.writeAtomically
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import app.melova.platform.PushAddress
import app.melova.platform.cacheDir
import kotlinx.io.IOException

/** Chats between students and teachers, and the takes sent in them. */
class ChatRepository(
    private val context: PlatformContext,
    private val api: ChatApi = MelovaNetwork.chatApi
) {
    private val session = TeacherSession(context)

    suspend fun inbox(): ChatInbox {
        val inbox = apiCall { api.chats(session.token) }
        return ChatInbox(inbox.chats.orEmpty().mapNotNull { it.toDomain() }, inbox.unread ?: 0)
    }

    /** The chat with a teacher, for writing to them from their page. Returns its id. */
    suspend fun startChat(teacherId: String): String =
        apiCall { api.start(session.token, teacherId) }.id ?: throw IOException("این گفتگو باز نشد.")

    suspend fun thread(chatId: String): ChatThread {
        val thread = apiCall { api.thread(session.token, chatId) }
        return ChatThread(thread.chat.required(), thread.messages.orEmpty().mapNotNull { it.toDomain() })
    }

    /** The person has the chat on screen up to this message. */
    suspend fun markRead(chatId: String, messageId: Long): ChatUpdate =
        apiCall { api.read(session.token, chatId, messageId) }.run { toUpdate(chat, messages, removed) }

    /** The key a browser needs before it gives out a push address. */
    suspend fun pushKey(): String = apiCall { api.pushKey() }.key ?: throw IOException("اعلان روشن نشد.")

    /** From now on this device hears about this person's messages. */
    suspend fun subscribe(address: PushAddress) = apiCall { api.subscribe(session.token, address) }

    suspend fun unsubscribe(address: PushAddress) = apiCall { api.unsubscribe(address) }

    /** What the server pushes, until the connection ends. [onReady] comes once it is listening. */
    suspend fun listen(onReady: () -> Unit, onUpdate: (ChatUpdate) -> Unit) {
        api.listen(session.token) { event ->
            when (event.type) {
                "ready" -> onReady()
                "chat" -> onUpdate(toUpdate(event.chat, event.messages, event.removed))
            }
        }
    }

    suspend fun sendText(chatId: String, text: String, replyTo: Long?): ChatUpdate =
        apiCall { api.send(session.token, chatId, text.trim(), durationMs = null, audio = null, replyTo = replyTo) }
            .run { toUpdate(chat, messages, removed) }

    /** [onProgress] follows the voice on its way out, from 0 to 1. */
    suspend fun sendVoice(
        chatId: String,
        voice: LocalFile,
        durationMs: Long,
        replyTo: Long?,
        onProgress: (Float) -> Unit
    ): ChatUpdate = apiCall {
        if (!voice.exists()) throw IOException("فایل پیام صوتی باز نشد.")
        val audio = FilePart("audio", voice.name, mediaTypeFor(voice.name), voice.readBytes())
        api.sendVoice(session.token, chatId, durationMs, audio, replyTo, onProgress)
    }.run { toUpdate(chat, messages, removed) }

    suspend fun editText(chatId: String, messageId: Long, text: String): ChatUpdate =
        apiCall { api.edit(session.token, chatId, messageId, text.trim()) }.run { toUpdate(chat, messages, removed) }

    suspend fun deleteMessage(chatId: String, messageId: Long): ChatUpdate =
        apiCall { api.delete(session.token, chatId, messageId) }.run { toUpdate(chat, messages, removed) }

    /** The teacher's «آفرین» on a take sent to them, or taking it back. */
    suspend fun stamp(chatId: String, messageId: Long, stamped: Boolean): ChatUpdate =
        apiCall { api.stamp(session.token, chatId, messageId, stamped) }.run { toUpdate(chat, messages, removed) }

    /** Whether this voice is already here, so it plays without waiting for the network. */
    fun hasVoice(message: ChatMessage): Boolean = voiceFile(message).let { it.exists() && it.length() > 0L }

    /** A voice message, downloaded once; [onProgress] follows it in, from 0 to 1. */
    suspend fun voice(chatId: String, message: ChatMessage, onProgress: (Float) -> Unit): LocalFile = apiCall {
        val target = voiceFile(message)
        if (!target.exists() || target.length() == 0L) {
            api.voice(session.token, chatId, message.id, onProgress).writeAtomically(target)
        }
        target
    }

    /**
     * A copy of a recording, kept until its message has gone. The recorder is given back as soon
     * as the take is sent, so the file it wrote is no longer there to read — or to send again.
     */
    fun keepForSending(localId: Long, voice: LocalFile): LocalFile {
        val dir = LocalFile(cacheDir(context), OUTBOX).apply { mkdirs() }
        return voice.copyTo(LocalFile(dir, "$localId.${voice.extension.ifEmpty { "m4a" }}"), overwrite = true)
    }

    /** The kept copy of a voice that has gone out, or that the person dropped. */
    fun dropOutgoing(voice: LocalFile?) {
        voice?.takeIf { it.path.contains("/$OUTBOX/") }?.delete()
    }

    /** A voice that has just gone out is already here: it plays without being fetched back. */
    fun keepSentVoice(message: ChatMessage, voice: LocalFile) {
        voice.copyTo(voiceFile(message), overwrite = true)
    }

    private fun voiceFile(message: ChatMessage): LocalFile {
        val dir = LocalFile(cacheDir(context), "chat_voices").apply { mkdirs() }
        return LocalFile(dir, "${message.id}.${message.audioExtension}")
    }

    /** Looking at a take as its teacher starts its clock on the server, as in the queue. */
    suspend fun take(reviewId: String): SharedTake {
        val take = apiCall { api.take(session.token, reviewId) }
        return SharedTake(
            isTeacherViewing = take.viewer == "teacher",
            studentName = take.studentName.orEmpty(),
            result = take.result?.toDomain()
        )
    }

    /** The recording the student scored, downloaded once. */
    suspend fun takeAudio(reviewId: String): LocalFile = apiCall {
        val dir = LocalFile(cacheDir(context), "review_takes").apply { mkdirs() }
        val target = LocalFile(dir, "${reviewId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.wav")
        if (!target.exists() || target.length() == 0L) {
            api.takeAudio(session.token, reviewId).writeAtomically(target)
        }
        target
    }

    private companion object {
        const val OUTBOX = "chat_outbox"
    }
}

private fun ChatDto?.required(): Chat = this?.toDomain() ?: throw IOException("این گفتگو باز نشد.")

private fun toUpdate(chat: ChatDto?, messages: List<ChatMessageDto>?, removed: List<Long>?): ChatUpdate =
    ChatUpdate(chat.required(), messages.orEmpty().mapNotNull { it.toDomain() }, removed.orEmpty())

private fun ChatDto.toDomain(): Chat? {
    val chatId = id?.takeIf { it.isNotBlank() } ?: return null
    return Chat(
        id = chatId,
        side = ChatSide.fromRaw(side),
        peerName = peerName.orEmpty(),
        teacherId = teacherId.orEmpty(),
        unread = unread ?: 0,
        peerReadId = peerReadId ?: 0L,
        lastMessage = lastMessage?.toDomain(),
        lastMessageAtMs = ((lastMessageAt ?: 0.0) * 1000).toLong()
    )
}

/** Null for a kind this version of the app doesn't know. */
private fun ChatMessageDto.toDomain(): ChatMessage? {
    return ChatMessage(
        id = id ?: return null,
        sender = ChatSide.fromRaw(sender),
        kind = ChatMessageKind.fromRaw(kind) ?: return null,
        text = text.orEmpty(),
        durationMs = durationMs,
        audioExtension = audioExtension.orEmpty().ifBlank { "m4a" },
        peaks = peaks.orEmpty(),
        createdAtMs = ((createdAt ?: 0.0) * 1000).toLong(),
        editedAtMs = editedAt?.let { (it * 1000).toLong() },
        replyToId = replyTo,
        practice = practice?.toDomain()
    )
}

private fun PracticeResultLinkDto.toDomain(): PracticeResultLink? {
    return PracticeResultLink(
        reviewId = reviewId ?: return null,
        lessonId = lessonId ?: return null,
        sectionId = sectionId ?: return null,
        lessonTitle = lessonTitle.orEmpty(),
        sectionTitle = sectionTitle.orEmpty(),
        score = score,
        status = ReviewStatus.fromRaw(status),
        stamped = stamped == true
    )
}
