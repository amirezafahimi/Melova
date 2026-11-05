package app.melova.data.remote

import app.melova.data.remote.MelovaNetwork.call
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod

internal actual suspend fun receiveWithProgress(
    path: String,
    headers: Map<String, String>,
    onProgress: (Float) -> Unit
): ByteArray = MelovaNetwork.teacherUploadClient.call(HttpMethod.Get, path) {
    headers.forEach { (name, value) -> header(name, value) }
    onDownload { received, total ->
        if (total != null && total > 0L) onProgress((received.toFloat() / total).coerceIn(0f, 1f))
    }
}.bodyAsBytes()
