package app.melova.data.remote

import app.melova.data.remote.MelovaNetwork.call
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod

internal actual suspend fun sendWithProgress(
    method: HttpMethod,
    path: String,
    headers: Map<String, String>,
    fields: Map<String, String?>,
    file: FilePart,
    onProgress: (Float) -> Unit
): String = MelovaNetwork.teacherUploadClient.call(method, path) {
    headers.forEach { (name, value) -> header(name, value) }
    setBody(multipartBody(fields, listOf(file)))
    onUpload { sent, total ->
        if (total != null && total > 0L) onProgress((sent.toFloat() / total).coerceIn(0f, 1f))
    }
}.bodyAsText()
