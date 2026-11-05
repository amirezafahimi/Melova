package app.melova.data.remote

import app.melova.audio.web.DownloadAnswerJs
import app.melova.audio.web.MelovaAudioJs
import app.melova.platform.apiBaseUrl
import app.melova.platform.toByteArray
import kotlinx.coroutines.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal actual suspend fun receiveWithProgress(
    path: String,
    headers: Map<String, String>,
    onProgress: (Float) -> Unit
): ByteArray {
    val allHeaders = headers + listOfNotNull(MelovaNetwork.studentToken?.let { MelovaNetwork.STUDENT_HEADER to it })
    val answer = MelovaAudioJs.download(
        url = apiBaseUrl + path,
        headersJson = allHeaders.toHeaderJson(),
        progress = { fraction -> onProgress(fraction.toFloat().coerceIn(0f, 1f)) }
    ).await<JsAny?>()?.unsafeCast<DownloadAnswerJs>()
        ?: throw ApiException("اتصال برقرار نشد؛ اینترنت را بررسی کنید.", null)
    if (answer.status !in 200..299) {
        throw ApiException("این فایل باز نشد (${answer.status})", answer.status)
    }
    return answer.bytes.toByteArray()
}

private fun Map<String, String>.toHeaderJson(): String =
    MelovaNetwork.json.encodeToString(JsonObject.serializer(), JsonObject(mapValues { JsonPrimitive(it.value) }))
