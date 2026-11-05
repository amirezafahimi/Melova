package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatsDto(
    @SerialName("chats") val chats: List<ChatDto>? = null,
    @SerialName("unread") val unread: Int? = null
)

@Serializable
data class ChatDto(
    @SerialName("id") val id: String?,
    @SerialName("side") val side: String? = null,
    @SerialName("peer_name") val peerName: String? = null,
    @SerialName("unread") val unread: Int? = null,
    @SerialName("peer_read_id") val peerReadId: Long? = null,
    @SerialName("last_message") val lastMessage: ChatMessageDto? = null,
    @SerialName("last_message_at") val lastMessageAt: Double? = null
)

/** A chat with its messages: all of them for a thread, those that changed for an update. */
@Serializable
data class ChatThreadDto(
    @SerialName("chat") val chat: ChatDto? = null,
    @SerialName("messages") val messages: List<ChatMessageDto>? = null,
    @SerialName("removed") val removed: List<Long>? = null
)

/** What arrives on the socket: ``ready``, ``heartbeat``, or ``chat`` with an update. */
@Serializable
data class ChatEventDto(
    @SerialName("type") val type: String? = null,
    @SerialName("chat") val chat: ChatDto? = null,
    @SerialName("messages") val messages: List<ChatMessageDto>? = null,
    @SerialName("removed") val removed: List<Long>? = null
)

/** The first frame on the socket, where a browser can't set headers. */
@Serializable
data class ChatHelloDto(
    @SerialName("student_token") val studentToken: String?,
    @SerialName("teacher_token") val teacherToken: String?
)

@Serializable
data class ChatMessageDto(
    @SerialName("id") val id: Long?,
    @SerialName("sender") val sender: String? = null,
    @SerialName("kind") val kind: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    @SerialName("audio_extension") val audioExtension: String? = null,
    @SerialName("peaks") val peaks: List<Float>? = null,
    @SerialName("created_at") val createdAt: Double? = null,
    @SerialName("edited_at") val editedAt: Double? = null,
    @SerialName("reply_to") val replyTo: Long? = null,
    @SerialName("practice") val practice: PracticeResultLinkDto? = null
)

@Serializable
data class PracticeResultLinkDto(
    @SerialName("review_id") val reviewId: String?,
    @SerialName("lesson_id") val lessonId: String?,
    @SerialName("section_id") val sectionId: String?,
    @SerialName("lesson_title") val lessonTitle: String? = null,
    @SerialName("section_title") val sectionTitle: String? = null,
    @SerialName("score") val score: Int? = null,
    @SerialName("status") val status: String? = null
)

@Serializable
data class SharedTakeDto(
    @SerialName("viewer") val viewer: String? = null,
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("result") val result: ScoringResponseDto? = null
)

@Serializable
data class PushKeyDto(@SerialName("key") val key: String? = null)
