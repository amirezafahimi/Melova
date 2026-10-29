package app.melova.data.remote

import app.melova.platform.ioDispatcher
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The HTTP status of a failed call, when the server answered at all. */
class ApiException(message: String, val code: Int?, cause: Throwable? = null) : IOException(message, cause)

/** Runs a call on IO and turns server errors into the Persian message the server sent. */
suspend fun <T> apiCall(block: suspend () -> T): T = withContext(ioDispatcher) {
    try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: ApiException) {
        throw error
    } catch (error: ResponseException) {
        val code = error.response.status.value
        throw ApiException(serverDetail(error) ?: "خطای سرور ($code)", code, error)
    } catch (error: HttpRequestTimeoutException) {
        throw ApiException("پاسخ سرور طول کشید؛ دوباره تلاش کنید.", null, error)
    } catch (error: ConnectTimeoutException) {
        throw ApiException("ارتباط با سرور برقرار نشد.", null, error)
    } catch (error: IOException) {
        throw ApiException("ارتباط با سرور برقرار نشد.", null, error)
    } catch (error: Exception) {
        throw ApiException("ارتباط با سرور برقرار نشد.", null, error)
    }
}

/** FastAPI's {"detail": "..."} of a failed call. */
suspend fun serverDetail(error: ResponseException): String? =
    runCatching {
        MelovaNetwork.json.parseToJsonElement(error.response.bodyAsText())
            .jsonObject["detail"]?.jsonPrimitive?.content
    }.getOrNull()

/** The raw body of a failed call, trimmed. */
suspend fun serverBody(error: ResponseException): String =
    runCatching { error.response.bodyAsText().trim() }.getOrDefault("")
