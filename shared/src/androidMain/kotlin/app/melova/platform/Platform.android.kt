package app.melova.platform

import android.content.Context
import app.melova.shared.BuildConfig
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual typealias PlatformContext = Context

actual val apiBaseUrl: String = BuildConfig.SCORING_API_BASE_URL

actual val isDebugBuild: Boolean = BuildConfig.DEBUG

actual val appVersionName: String
    get() = MelovaPlatform.context.run { packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() }

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun nanoTime(): Long = System.nanoTime()

actual fun filesDir(context: PlatformContext): LocalFile = LocalFile(context.filesDir.absolutePath)

actual fun cacheDir(context: PlatformContext): LocalFile = LocalFile(context.cacheDir.absolutePath)

actual fun <K, V> concurrentMapOf(): MutableMap<K, V> = ConcurrentHashMap()

/** The real file, for Android APIs (MediaRecorder, ExoPlayer) that need one. */
val LocalFile.javaFile: java.io.File
    get() = java.io.File(path)

fun java.io.File.toLocalFile(): LocalFile = LocalFile(absolutePath)

actual typealias SecurityException = java.lang.SecurityException
