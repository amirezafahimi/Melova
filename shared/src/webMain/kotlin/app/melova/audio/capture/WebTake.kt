package app.melova.audio.capture

import app.melova.audio.web.FinishedTakeJs
import app.melova.audio.web.JsTake
import app.melova.platform.toByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await

/** Turns a browser failure (a JS Error) into the message the app shows. */
internal suspend fun <T> browserAudio(block: suspend () -> T): T = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    throw IllegalStateException(error.message?.substringAfter("Error: ")?.takeIf { it.isNotBlank() } ?: "میکروفون در دسترس نیست", error)
}

internal suspend fun JsTake.finishBytes(): Pair<ByteArray, String> {
    val finished = finish().await<JsAny?>()!!.unsafeCast<FinishedTakeJs>()
    return finished.bytes.toByteArray() to finished.extension
}
