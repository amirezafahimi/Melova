package app.melova.data.remote

import app.melova.audio.web.MelovaAudioJs
import app.melova.audio.web.UploadAnswerJs
import app.melova.platform.apiBaseUrl
import app.melova.platform.persianDigits
import app.melova.platform.toInt8Array
import io.ktor.http.HttpMethod
import kotlinx.coroutines.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal actual suspend fun sendWithProgress(
    method: HttpMethod,
    path: String,
    headers: Map<String, String>,
    fields: Map<String, String?>,
    file: FilePart,
    onProgress: (Float) -> Unit
): String {
    val allHeaders = headers + listOfNotNull(MelovaNetwork.studentToken?.let { MelovaNetwork.STUDENT_HEADER to it })
    val answer = MelovaAudioJs.upload(
        method = method.value,
        url = apiBaseUrl + path,
        headersJson = allHeaders.toJsonText(),
        fieldsJson = fields.filterValues { it != null }.mapValues { it.value.orEmpty() }.toJsonText(),
        fileField = file.name,
        fileName = file.fileName,
        mimeType = file.mimeType,
        bytes = file.bytes.toInt8Array(),
        progress = { fraction -> onProgress(fraction.toFloat().coerceIn(0f, 1f)) }
    ).await<JsAny?>()?.unsafeCast<UploadAnswerJs>() ?: throw ApiException("اتصال برقرار نشد؛ اینترنت را بررسی کنید.", null)
    if (answer.status !in 200..299) {
        val detail = runCatching {
            MelovaNetwork.json.parseToJsonElement(answer.body).jsonObject["detail"]?.jsonPrimitive?.content
        }.getOrNull()
        throw ApiException(persianDigits(detail ?: "خطایی پیش آمد (${answer.status})"), answer.status)
    }
    return answer.body
}

private fun Map<String, String>.toJsonText(): String =
    MelovaNetwork.json.encodeToString(JsonObject.serializer(), JsonObject(mapValues { JsonPrimitive(it.value) }))
