package app.melova.audio.capture

import app.melova.audio.web.JsTake
import app.melova.audio.web.MelovaAudioJs
import app.melova.audio.io.WavUtils
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import kotlin.js.Promise
import kotlinx.coroutines.await

internal actual fun losslessTakeRecorder(context: PlatformContext): TeacherTakeRecorder =
    WebTakeRecorder("wav") { MelovaAudioJs.startCapture(WavUtils.SAMPLE_RATE) }

internal actual fun compressedTakeRecorder(context: PlatformContext): TeacherTakeRecorder =
    WebTakeRecorder(MelovaAudioJs.compressedExtension()) { MelovaAudioJs.startCompressed() }

/** The take stays in the browser until [stop], which writes it to [LocalFile]. */
private class WebTakeRecorder(
    override val extension: String,
    private val open: () -> Promise<JsAny?>
) : TeacherTakeRecorder {
    private var take: JsTake? = null
    private var output: LocalFile? = null
    private var stoppedAtMs = 0L

    override val elapsedMs: Long
        get() = take?.elapsedMs()?.toLong() ?: stoppedAtMs

    override val level: Float
        get() = take?.level()?.toFloat() ?: 0f

    override val error: String?
        get() = take?.error()

    override suspend fun start(output: LocalFile) {
        take = browserAudio { open().await<JsAny?>()!!.unsafeCast<JsTake>() }
        this.output = output
        stoppedAtMs = 0L
    }

    override fun pause() {
        take?.pause()
    }

    override fun resume() {
        take?.resume()
    }

    override suspend fun stop(): Long {
        val active = take ?: return 0L
        active.error()?.let { message ->
            cancel()
            throw IllegalStateException(message)
        }
        stoppedAtMs = active.elapsedMs().toLong()
        val (bytes, _) = active.finishBytes()
        take = null
        output?.writeBytes(bytes)
        if (stoppedAtMs < 500L) throw IllegalStateException("ضبط خیلی کوتاه بود")
        return stoppedAtMs
    }

    override fun cancel() {
        take?.cancel()
        take = null
        output?.delete()
    }
}
