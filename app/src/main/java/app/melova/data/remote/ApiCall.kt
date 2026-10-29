package app.melova.data.remote

import com.google.gson.JsonParser
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/** The HTTP status of a failed call, when the server answered at all. */
class ApiException(message: String, val code: Int?, cause: Throwable? = null) : IOException(message, cause)

/** Runs a call on IO and turns server errors into the Persian message the server sent. */
suspend fun <T> apiCall(block: suspend () -> T): T = withContext(Dispatchers.IO) {
    try {
        block()
    } catch (error: HttpException) {
        val detail = error.response()?.errorBody()?.string()?.let { body ->
            runCatching { JsonParser.parseString(body).asJsonObject.get("detail").asString }.getOrNull()
        }
        throw ApiException(detail ?: "خطای سرور (${error.code()})", error.code(), error)
    } catch (error: java.net.ConnectException) {
        throw ApiException("ارتباط با سرور برقرار نشد.", null, error)
    } catch (error: java.net.SocketTimeoutException) {
        throw ApiException("پاسخ سرور طول کشید؛ دوباره تلاش کنید.", null, error)
    } catch (error: java.net.UnknownHostException) {
        throw ApiException("اتصال اینترنت را بررسی کنید.", null, error)
    } catch (error: IOException) {
        throw error
    } catch (error: Exception) {
        throw ApiException("ارتباط با سرور برقرار نشد.", null, error)
    }
}
