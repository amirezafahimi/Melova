package app.melova.audio.web

import kotlin.js.Promise
import org.khronos.webgl.Int8Array

/** A recording in progress, from melova-audio.js. */
external interface JsTake : JsAny {
    fun elapsedMs(): Double
    fun level(): Double
    fun error(): String?
    fun pause()
    fun resume()

    /** Resolves to a [FinishedTakeJs]. */
    fun finish(): Promise<JsAny?>
    fun cancel()
}

external interface FinishedTakeJs : JsAny {
    val bytes: Int8Array
    val extension: String
}

external interface PickedFileJs : JsAny {
    val name: String
    val bytes: Int8Array
}

external interface GoogleAnswerJs : JsAny {
    val credential: String?
    val cancelled: Boolean
}

external interface JsPlayer : JsAny {
    fun load(bytes: Int8Array, name: String, finished: () -> Unit, ready: () -> Unit)
    fun play()
    fun pause()
    fun seek(ms: Double)
    fun positionMs(): Double
    fun durationMs(): Double
    fun isPlaying(): Boolean
    fun hasEnded(): Boolean
    fun isReady(): Boolean
    fun setSpeed(speed: Double)
    fun setLoop(loop: Boolean)
    fun stop()
}

/** globalThis.MelovaAudio, set up by melova-audio.js before the app starts. */
@JsName("MelovaAudio")
private external object MelovaAudioGlobal : JsAny {
    fun startCapture(sampleRate: Int): Promise<JsAny?>
    fun startCompressed(): Promise<JsAny?>
    fun compressedExtension(): String
    fun createPlayer(): JsPlayer
    fun pickFile(accept: String): Promise<JsAny?>
    fun googleSignIn(clientId: String): Promise<JsAny?>
    fun apiBaseUrl(): String
    fun canInstall(): Boolean
    fun watchInstall(changed: () -> Unit)
    fun install()
    fun pushState(): Promise<JsAny?>
    fun pushAddress(): Promise<JsAny?>
    fun pushSubscribe(key: String): Promise<JsAny?>
    fun pushUnsubscribe(): Promise<JsAny?>
    fun askNotifications(): Promise<JsAny?>
    fun watchChatLinks(opened: (String) -> Unit)
    fun pushAllowed(): Boolean
    fun onFirstTap(tapped: () -> Unit)
}

object MelovaAudioJs {
    /** Resolves to a [JsTake]. */
    fun startCapture(sampleRate: Int): Promise<JsAny?> = MelovaAudioGlobal.startCapture(sampleRate)

    /** Resolves to a [JsTake]. */
    fun startCompressed(): Promise<JsAny?> = MelovaAudioGlobal.startCompressed()

    /** What [startCompressed] will write in this browser: m4a, ogg or webm. */
    fun compressedExtension(): String = MelovaAudioGlobal.compressedExtension()

    fun createPlayer(): JsPlayer = MelovaAudioGlobal.createPlayer()

    /** Resolves to a [PickedFileJs], or null when cancelled. */
    fun pickFile(accept: String): Promise<JsAny?> = MelovaAudioGlobal.pickFile(accept)

    /** Resolves to a [GoogleAnswerJs]; rejects when Google's script can't load. */
    fun googleSignIn(clientId: String): Promise<JsAny?> = MelovaAudioGlobal.googleSignIn(clientId)

    /** MELOVA_API_BASE_URL from the page, or empty. */
    fun apiBaseUrl(): String = MelovaAudioGlobal.apiBaseUrl()

    /** Whether the browser is offering to install the app right now. */
    fun canInstall(): Boolean = MelovaAudioGlobal.canInstall()

    /** Calls [changed] whenever [canInstall] may have changed. */
    fun watchInstall(changed: () -> Unit) = MelovaAudioGlobal.watchInstall(changed)

    fun install() = MelovaAudioGlobal.install()

    /** Resolves to on, off, blocked, needs-install or unsupported. */
    fun pushState(): Promise<JsAny?> = MelovaAudioGlobal.pushState()

    /** Resolves to this browser's address as JSON, or null when it has none. */
    fun pushAddress(): Promise<JsAny?> = MelovaAudioGlobal.pushAddress()

    /** Resolves to the new address as JSON. */
    fun pushSubscribe(key: String): Promise<JsAny?> = MelovaAudioGlobal.pushSubscribe(key)

    /** Resolves to the address that stopped, as JSON, or null when there was none. */
    fun pushUnsubscribe(): Promise<JsAny?> = MelovaAudioGlobal.pushUnsubscribe()

    /** Resolves to whether the person allowed notifications. */
    fun askNotifications(): Promise<JsAny?> = MelovaAudioGlobal.askNotifications()

    /** Calls [opened] with the chat of each tapped notification, including the one that opened the page. */
    fun watchChatLinks(opened: (String) -> Unit) = MelovaAudioGlobal.watchChatLinks(opened)

    /** Whether this browser already lets the app show notifications. */
    fun pushAllowed(): Boolean = MelovaAudioGlobal.pushAllowed()

    /** [tapped] runs inside the person's first tap, where browsers accept a permission question. */
    fun onFirstTap(tapped: () -> Unit) = MelovaAudioGlobal.onFirstTap(tapped)
}
