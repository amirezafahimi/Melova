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

object MelovaAudioJs {
    /** Resolves to a [JsTake]. */
    fun startCapture(sampleRate: Int): Promise<JsAny?> = jsStartCapture(sampleRate)

    /** Resolves to a [JsTake]. */
    fun startCompressed(): Promise<JsAny?> = jsStartCompressed()

    /** What [startCompressed] will write in this browser: m4a, ogg or webm. */
    fun compressedExtension(): String = jsCompressedExtension()

    fun createPlayer(): JsPlayer = jsCreatePlayer()

    /** Resolves to a [PickedFileJs], or null when cancelled. */
    fun pickAudioFile(): Promise<JsAny?> = jsPickAudioFile()
}

@JsFun("(rate) => globalThis.MelovaAudio.startCapture(rate)")
private external fun jsStartCapture(sampleRate: Int): Promise<JsAny?>

@JsFun("() => globalThis.MelovaAudio.startCompressed()")
private external fun jsStartCompressed(): Promise<JsAny?>

@JsFun("() => globalThis.MelovaAudio.createPlayer()")
private external fun jsCreatePlayer(): JsPlayer

@JsFun("() => globalThis.MelovaAudio.pickAudioFile()")
private external fun jsPickAudioFile(): Promise<JsAny?>

@JsFun("() => globalThis.MelovaAudio.compressedExtension()")
private external fun jsCompressedExtension(): String
