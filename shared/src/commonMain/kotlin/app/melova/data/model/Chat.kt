package app.melova.data.model

/** Who a message is from, and which side of a chat someone is on. */
enum class ChatSide {
    STUDENT,
    TEACHER;

    companion object {
        fun fromRaw(raw: String?): ChatSide = if (raw == "teacher") TEACHER else STUDENT
    }
}

enum class ChatMessageKind {
    TEXT,
    VOICE,

    /** A practice result the student sent; tapping it opens that result page. */
    PRACTICE;

    companion object {
        fun fromRaw(raw: String?): ChatMessageKind? = when (raw) {
            "text" -> TEXT
            "voice" -> VOICE
            "practice" -> PRACTICE
            else -> null
        }
    }
}

/** The take behind a practice message. */
data class PracticeResultLink(
    val reviewId: String,
    val lessonId: String,
    val sectionId: String,
    val lessonTitle: String,
    val sectionTitle: String,
    /** Null when the scorer turned the take down. */
    val score: Int?,
    val status: ReviewStatus
)

data class ChatMessage(
    val id: Long,
    val sender: ChatSide,
    val kind: ChatMessageKind,
    val text: String,
    /** Null only for a voice from the review queue that the server couldn't read. */
    val durationMs: Long?,
    val audioExtension: String,
    /** The bars of a voice message's wave, from 0 to 1; empty when the server couldn't read it. */
    val peaks: List<Float>,
    val createdAtMs: Long,
    /** When the sender rewrote it; null for one that stands as it was written. */
    val editedAtMs: Long?,
    /** The message in this chat that this one answers, when it answers one. */
    val replyToId: Long?,
    val practice: PracticeResultLink?
)

/** A student and a teacher; each pair has one chat. */
data class Chat(
    val id: String,
    /** Which side the person looking at it is on. */
    val side: ChatSide,
    val peerName: String,
    /** The teacher in this chat; their lessons' messages left show on the student's side. */
    val teacherId: String = "",
    val unread: Int,
    /** The other side has seen every message up to this id. */
    val peerReadId: Long,
    val lastMessage: ChatMessage?,
    val lastMessageAtMs: Long
)

data class ChatInbox(val chats: List<Chat>, val unread: Int)

data class ChatThread(val chat: Chat, val messages: List<ChatMessage>)

/**
 * A chat as it now stands, with the messages that are new or different and the ids of any the
 * sender took back.
 */
data class ChatUpdate(val chat: Chat, val messages: List<ChatMessage>, val removed: List<Long> = emptyList())

/** A take as the student saw it, opened from its message. */
data class SharedTake(
    val isTeacherViewing: Boolean,
    val studentName: String,
    /** Null when the take came without a readable result. */
    val result: VocalScoreResult?
)
