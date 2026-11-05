package app.melova.data.remote

import io.ktor.http.HttpMethod

/**
 * A multipart request to [path] (relative, like [MelovaNetwork.call]) with one [file], calling
 * [onProgress] with how much of it has gone, from 0 to 1, and returning the response body.
 * Ktor in the browser reads the whole body before fetch sends it, so its progress would jump
 * straight to the end; the web build sends it with XMLHttpRequest, which reports it truly.
 * A failed status throws, and [apiCall] turns it into the server's own message.
 */
internal expect suspend fun sendWithProgress(
    method: HttpMethod,
    path: String,
    headers: Map<String, String>,
    fields: Map<String, String?>,
    file: FilePart,
    onProgress: (Float) -> Unit
): String
