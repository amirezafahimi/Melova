package app.melova.data.remote

import app.melova.data.remote.api.MelovaApi
import app.melova.data.remote.api.StudentApi
import app.melova.data.remote.api.TeacherApi
import app.melova.data.remote.api.TeacherDeskApi
import app.melova.platform.apiBaseUrl
import app.melova.platform.isDebugBuild
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.encodeURLPathPart
import kotlin.concurrent.Volatile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object MelovaNetwork {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /**
     * This install's student token, sent as X-Student-Token on every request so the server
     * knows which lessons are open to the student. Set from [app.melova.data.local.StudentSession].
     */
    @Volatile
    var studentToken: String? = null

    /** Asset downloads (teacher wav/json). */
    val api: MelovaApi by lazy { MelovaApi(client(requestTimeoutSeconds = 120)) }

    /** Scoring uploads can take longer — separate client with extended timeouts. */
    val scoringApi: MelovaApi by lazy { MelovaApi(client(requestTimeoutSeconds = 300, connectTimeoutSeconds = 60)) }

    /** Teacher panel; uploads of lesson audio can be large. */
    val teacherApi: TeacherApi by lazy { TeacherApi(teacherUploadClient) }

    val studentApi: StudentApi by lazy { StudentApi(teacherUploadClient) }

    val teacherDeskApi: TeacherDeskApi by lazy { TeacherDeskApi(teacherUploadClient) }

    /** Takes and voice replies go up and down through these; give them time. */
    private val teacherUploadClient: HttpClient by lazy { client(requestTimeoutSeconds = 300) }

    private const val STUDENT_HEADER = "X-Student-Token"

    private fun client(requestTimeoutSeconds: Long, connectTimeoutSeconds: Long = 30): HttpClient =
        HttpClient(httpEngine()) {
            expectSuccess = true
            install(HttpTimeout) {
                connectTimeoutMillis = connectTimeoutSeconds * 1000
                requestTimeoutMillis = requestTimeoutSeconds * 1000
                socketTimeoutMillis = requestTimeoutSeconds * 1000
            }
            if (isDebugBuild) {
                install(Logging) { level = LogLevel.INFO }
            }
        }

    /** One call to the server at [path] (relative, without a leading slash). */
    internal suspend fun HttpClient.call(
        method: HttpMethod,
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse = request {
        this.method = method
        url(apiBaseUrl + path)
        studentToken?.let { header(STUDENT_HEADER, it) }
        block()
    }

    internal fun HttpRequestBuilder.jsonBody(body: JsonElement) =
        setBody(TextContent(json.encodeToString(JsonElement.serializer(), body), ContentType.Application.Json))

    internal suspend inline fun <reified T> HttpResponse.decode(): T = json.decodeFromString(bodyAsText())

    /** A path segment from an id, escaped like Retrofit's @Path. */
    internal fun segment(raw: String): String = raw.encodeURLPathPart()
}
