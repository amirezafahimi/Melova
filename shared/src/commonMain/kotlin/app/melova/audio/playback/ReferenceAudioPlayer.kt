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

    /** سرعت پخش؛ زیر و بمی صدا عوض نمی‌شود تا فالشی همان‌طور که بوده شنیده شود. */
    fun setSpeed(speed: Float)

    /** پخش دوباره‌ی همان فایل از ابتدا تا وقتی خاموش شود (برای تکرار یک بازه). */
    fun setLooping(loop: Boolean)

    fun currentPositionMs(): Long
    fun durationMs(): Long
    fun currentProgress(): Float
    fun release()
}
