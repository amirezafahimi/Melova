package app.melova.data.remote.api

import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.MelovaNetwork.call
import app.melova.data.remote.MelovaNetwork.decode
import app.melova.data.remote.MelovaNetwork.jsonBody
import app.melova.data.remote.MelovaNetwork.segment
import app.melova.data.remote.dto.ChatDto
import app.melova.data.remote.dto.ChatEventDto
import app.melova.data.remote.dto.ChatHelloDto
import app.melova.data.remote.dto.ChatThreadDto
import app.melova.data.remote.dto.ChatsDto
import app.melova.data.remote.dto.PushKeyDto
import app.melova.data.remote.dto.SharedTakeDto
import app.melova.data.remote.multipartBody
import app.melova.data.remote.receiveWithProgress
import app.melova.data.remote.sendWithProgress
import app.melova.platform.PushAddress
import app.melova.platform.apiBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Chats, for both sides. The student token goes on every request; [teacherToken] is there for
 * a teacher who came in with the teacher code rather than an account.
 */
class ChatApi(private val client: HttpClient, private val socketClient: HttpClient) {
    suspend fun chats(teacherToken: String?): ChatsDto =
        client.call(HttpMethod.Get, "api/v1/chats") { header(TEACHER_TOKEN, teacherToken) }.decode()

    suspend fun start(teacherToken: String?, teacherId: String): ChatDto =
        client.call(HttpMethod.Post, "api/v1/chats") {
            header(TEACHER_TOKEN, teacherToken)
            jsonBody(mapOf("teacher_id" to teacherId).toJson())
        }.decode()

    suspend fun thread(teacherToken: String?, chatId: String): ChatThreadDto =
        client.call(HttpMethod.Get, "api/v1/chats/${segment(chatId)}") { header(TEACHER_TOKEN, teacherToken) }.decode()

    suspend fun read(teacherToken: String?, chatId: String, messageId: Long): ChatThreadDto =
        client.call(HttpMethod.Post, "api/v1/chats/${segment(chatId)}/read") {
            header(TEACHER_TOKEN, teacherToken)
            jsonBody(JsonObject(mapOf("message_id" to JsonPrimitive(messageId))))
        }.decode()

    suspend fun pushKey(): PushKeyDto = client.call(HttpMethod.Get, "api/v1/push/key").decode()

    suspend fun subscribe(teacherToken: String?, address: PushAddress) {
        client.call(HttpMethod.Post, "api/v1/push/subscriptions") {
            header(TEACHER_TOKEN, teacherToken)
            jsonBody(mapOf("endpoint" to address.endpoint, "p256dh" to address.p256dh, "auth" to address.auth).toJson())
        }
    }

    suspend fun unsubscribe(address: PushAddress) {
        client.call(HttpMethod.Post, "api/v1/push/subscriptions/remove") {
            jsonBody(mapOf("endpoint" to address.endpoint).toJson())
        }
    }

    /** Hands over what the server pushes, and returns once the connection has ended. */
    suspend fun listen(teacherToken: String?, onEvent: (ChatEventDto) -> Unit) {
        socketClient.webSocket(apiBaseUrl.replaceFirst("http", "ws") + "api/v1/chats/live") {
            val hello = ChatHelloDto(MelovaNetwork.studentToken, teacherToken)
            send(MelovaNetwork.json.encodeToString(ChatHelloDto.serializer(), hello))
            while (true) {
                // The server sends a heartbeat, so a silence this long is a dead connection.
                val frame = withTimeoutOrNull(SILENCE_LIMIT_MS) { incoming.receiveCatching().getOrNull() } ?: break
                if (frame is Frame.Text) onEvent(MelovaNetwork.json.decodeFromString(frame.readText()))
            }
        }
    }

    suspend fun send(
        teacherToken: String?,
        chatId: String,
        text: String,
        durationMs: Long?,
        audio: FilePart?
    ): ChatThreadDto = client.call(HttpMethod.Post, "api/v1/chats/${segment(chatId)}/messages") {
        header(TEACHER_TOKEN, teacherToken)
        setBody(
            multipartBody(
                mapOf("text" to text, "duration_ms" to durationMs?.toString()),
                listOfNotNull(audio)
            )
        )
    }.decode()

    /** A voice message, with the share of it that has gone reported as it goes. */
    suspend fun sendVoice(
        teacherToken: String?,
        chatId: String,
        durationMs: Long,
        audio: FilePart,
        onProgress: (Float) -> Unit
    ): ChatThreadDto = MelovaNetwork.json.decodeFromString(
        sendWithProgress(
            HttpMethod.Post,
            "api/v1/chats/${segment(chatId)}/messages",
            headerMap(teacherToken),
            mapOf("text" to "", "duration_ms" to durationMs.toString()),
            audio,
            onProgress
        )
    )

    suspend fun edit(teacherToken: String?, chatId: String, messageId: Long, text: String): ChatThreadDto =
        client.call(HttpMethod.Patch, "api/v1/chats/${segment(chatId)}/messages/$messageId") {
            header(TEACHER_TOKEN, teacherToken)
            jsonBody(JsonObject(mapOf("text" to JsonPrimitive(text))))
        }.decode()

    suspend fun delete(teacherToken: String?, chatId: String, messageId: Long): ChatThreadDto =
        client.call(HttpMethod.Delete, "api/v1/chats/${segment(chatId)}/messages/$messageId") {
            header(TEACHER_TOKEN, teacherToken)
        }.decode()

    /** A voice message, with the share of it that has arrived reported as it comes. */
    suspend fun voice(
        teacherToken: String?,
        chatId: String,
        messageId: Long,
        onProgress: (Float) -> Unit
    ): ByteArray = receiveWithProgress(
        "api/v1/chats/${segment(chatId)}/messages/$messageId/audio",
        headerMap(teacherToken),
        onProgress
    )

    /** The teacher's token only when there is one; these calls take their headers as a map. */
    private fun headerMap(teacherToken: String?): Map<String, String> =
        teacherToken?.let { mapOf(TEACHER_TOKEN to it) }.orEmpty()

    suspend fun take(teacherToken: String?, reviewId: String): SharedTakeDto =
        client.call(HttpMethod.Get, "api/v1/reviews/${segment(reviewId)}") { header(TEACHER_TOKEN, teacherToken) }.decode()

    suspend fun takeAudio(teacherToken: String?, reviewId: String): ByteArray =
        client.call(HttpMethod.Get, "api/v1/reviews/${segment(reviewId)}/audio") {
            header(TEACHER_TOKEN, teacherToken)
        }.bodyAsBytes()

    private companion object {
        const val SILENCE_LIMIT_MS = 60_000L
    }
}
