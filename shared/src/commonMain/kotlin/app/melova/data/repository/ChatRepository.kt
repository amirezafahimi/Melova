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
        apiCall { api.read(session.token, chatId, messageId) }.run { toUpdate(chat, messages) }

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
                "chat" -> onUpdate(toUpdate(event.chat, event.messages))
            }
        }
    }

    suspend fun sendText(chatId: String, text: String): ChatUpdate =
        apiCall { api.send(session.token, chatId, text.trim(), durationMs = null, audio = null) }
            .run { toUpdate(chat, messages) }

    suspend fun sendVoice(chatId: String, voice: LocalFile, durationMs: Long): ChatUpdate = apiCall {
        if (!voice.exists()) throw IOException("فایل پیام صوتی باز نشد.")
        val audio = FilePart("audio", voice.name, mediaTypeFor(voice.name), voice.readBytes())
        api.send(session.token, chatId, "", durationMs, audio)
    }.run { toUpdate(chat, messages) }

    /** A voice message, downloaded once. */
    suspend fun voice(chatId: String, message: ChatMessage): LocalFile = apiCall {
        val dir = LocalFile(cacheDir(context), "chat_voices").apply { mkdirs() }
        val target = LocalFile(dir, "${message.id}.${message.audioExtension}")
        if (!target.exists() || target.length() == 0L) {
            api.voice(session.token, chatId, message.id).writeAtomically(target)
        }
        target
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
}

private fun ChatDto?.required(): Chat = this?.toDomain() ?: throw IOException("این گفتگو باز نشد.")

private fun toUpdate(chat: ChatDto?, messages: List<ChatMessageDto>?): ChatUpdate =
    ChatUpdate(chat.required(), messages.orEmpty().mapNotNull { it.toDomain() })

private fun ChatDto.toDomain(): Chat? {
    val chatId = id?.takeIf { it.isNotBlank() } ?: return null
    return Chat(
        id = chatId,
        side = ChatSide.fromRaw(side),
        peerName = peerName.orEmpty(),
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
        status = ReviewStatus.fromRaw(status)
    )
}
