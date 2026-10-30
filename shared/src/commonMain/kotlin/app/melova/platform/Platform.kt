package app.melova.platform

import kotlinx.coroutines.CoroutineDispatcher

/** The Android Context on the phone; a placeholder in the browser, where nothing needs one. */
expect abstract class PlatformContext

/** Set once at start, before any screen: MainActivity on Android, main() on the web. */
object MelovaPlatform {
    lateinit var context: PlatformContext
}

/** Shorthand for [MelovaPlatform.context], for code that used to call getApplication(). */
val appContext: PlatformContext
    get() = MelovaPlatform.context

/** The scoring server, ending in a slash. */
expect val apiBaseUrl: String

expect val isDebugBuild: Boolean

/** Shown on the profile screen. */
expect val appVersionName: String

/**
 * Whether the player can change speed mid-play without a gap. Browsers restart their
 * pitch-keeping stretcher on each change, so there a slowed clip plays at one speed.
 */
expect val smoothSpeedChanges: Boolean

/** Google won't sign in on a page served from a bare IP address, only from a real domain. */
expect val supportsGoogleSignIn: Boolean

/** Blocking file and network work; the browser has one thread, so there it is the default. */
expect val ioDispatcher: CoroutineDispatcher

expect fun currentTimeMillis(): Long

expect fun nanoTime(): Long

/** App-private storage that survives restarts (Android filesDir). */
expect fun filesDir(context: PlatformContext): LocalFile

/** Storage the system may clear (Android cacheDir). */
expect fun cacheDir(context: PlatformContext): LocalFile

/** A map safe to use from several threads. */
expect fun <K, V> concurrentMapOf(): MutableMap<K, V>

/** Thrown when the microphone permission is missing (java.lang.SecurityException on Android). */
expect open class SecurityException : RuntimeException
