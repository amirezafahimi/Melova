package app.melova.platform

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual abstract class PlatformContext

/** The browser has nothing like an Android Context; this stands in for it. */
object WebContext : PlatformContext()

@JsFun("() => (globalThis.MELOVA_API_BASE_URL || '')")
private external fun configuredBaseUrl(): String

/** Same origin as the page unless index.html sets MELOVA_API_BASE_URL. */
actual val apiBaseUrl: String = configuredBaseUrl().ifBlank { window.location.origin }
    .let { if (it.endsWith("/")) it else "$it/" }

actual val isDebugBuild: Boolean = window.location.hostname == "localhost"

actual val appVersionName: String = "وب"

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

@JsFun("() => Date.now()")
private external fun dateNow(): Double

@JsFun("() => performance.now()")
private external fun performanceNow(): Double

actual fun currentTimeMillis(): Long = dateNow().toLong()

actual fun nanoTime(): Long = (performanceNow() * 1_000_000.0).toLong()

actual fun filesDir(context: PlatformContext): LocalFile = LocalFile("/files")

actual fun cacheDir(context: PlatformContext): LocalFile = LocalFile("/cache")

actual fun <K, V> concurrentMapOf(): MutableMap<K, V> = mutableMapOf()

actual open class SecurityException(message: String? = null) : RuntimeException(message)
