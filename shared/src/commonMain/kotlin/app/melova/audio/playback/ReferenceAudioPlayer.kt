package app.melova.audio.playback

import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext

/** Plays one audio file at a time, with seeking, looping and pitch-preserving speed. */
expect class ReferenceAudioPlayer(context: PlatformContext) {
    val isPlaying: Boolean

    /** Played through to the end (and not restarted since). */
    val hasEnded: Boolean

    val canResume: Boolean

    fun prepare(file: LocalFile, onFinished: () -> Unit, onReady: () -> Unit = {})
    fun seekTo(positionMs: Long)
    fun play()
    fun playFrom(positionMs: Long)
    fun pause()
    fun togglePlayPause()
    fun stop()

    /** Playback speed; the pitch stays, so an off note is heard as it was sung. */
    fun setSpeed(speed: Float)

    /** Plays the same file over from the start until turned off, to repeat a segment. */
    fun setLooping(loop: Boolean)

    fun currentPositionMs(): Long
    fun durationMs(): Long
    fun currentProgress(): Float
    fun release()
}
