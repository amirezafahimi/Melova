package app.melova.platform

import app.melova.audio.web.MelovaAudioJs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual abstract class PlatformContext

/** The browser has nothing like an Android Context; this stands in for it. */
object WebContext : PlatformContext()

/** Same origin as the page unless index.html sets MELOVA_API_BASE_URL. */
actual val apiBaseUrl: String = MelovaAudioJs.apiBaseUrl().ifBlank { window.location.origin }
    .let { if (it.endsWith("/")) it else "$it/" }

actual val isDebugBuild: Boolean = window.location.hostname == "localhost"

actual val appVersionName: String = "وب"

actual val smoothSpeedChanges: Boolean = false

actual val supportsGoogleSignIn: Boolean = !Regex("""\d+\.\d+\.\d+\.\d+""").matches(window.location.hostname)

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

@OptIn(ExperimentalTime::class)
actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

@JsName("Date")
private external class JsDate(timeMs: Double) : JsAny {
    fun getTimezoneOffset(): Double
}

actual fun utcOffsetMinutes(timeMs: Long): Int = -JsDate(timeMs.toDouble()).getTimezoneOffset().toInt()

private val startMark = TimeSource.Monotonic.markNow()

actual fun nanoTime(): Long = startMark.elapsedNow().inWholeNanoseconds

actual fun filesDir(context: PlatformContext): LocalFile = LocalFile("/files")

actual fun cacheDir(context: PlatformContext): LocalFile = LocalFile("/cache")

actual fun <K, V> concurrentMapOf(): MutableMap<K, V> = mutableMapOf()

actual open class SecurityException(message: String? = null) : RuntimeException(message)
