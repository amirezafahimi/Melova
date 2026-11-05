package app.melova.data.remote

/**
 * A GET of [path] (relative, like [MelovaNetwork.call]) that calls [onProgress] with how much
 * of the body has arrived, from 0 to 1, and returns it. As with [sendWithProgress], Ktor in the
 * browser hands over the whole body at once, so the web build reads it with XMLHttpRequest.
 * A failed status throws, and [apiCall] turns it into the server's own message.
 */
internal expect suspend fun receiveWithProgress(
    path: String,
    headers: Map<String, String>,
    onProgress: (Float) -> Unit
): ByteArray
