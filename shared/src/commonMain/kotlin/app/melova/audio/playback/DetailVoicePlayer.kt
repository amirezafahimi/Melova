package app.melova.audio.playback

import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext

/**
 * One voice in «دیدن جزییات», held to a stretch of its file. The file stays loaded and the speed
 * stays set, so play, pause and seek act at once: reloading a file, or changing speed while it
 * sounds, is what makes a player lag and jump.
 */
class DetailVoicePlayer(context: PlatformContext) {
    private val player = ReferenceAudioPlayer(context)
    private var startMs = 0L
    private var endMs = 0L
    private var headMs = 0L
    private var isLoaded = false

    fun load(file: LocalFile, startMs: Long, endMs: Long, speed: Float) {
        this.startMs = startMs
        this.endMs = endMs
        headMs = startMs
        isLoaded = true
        player.prepare(file, onFinished = {}, onReady = { player.seekTo(headMs) })
        player.seekTo(startMs)
        player.setSpeed(speed)
    }

    val positionMs: Long
        get() = if (isLoaded && player.durationMs() > 0L) player.currentPositionMs().coerceIn(startMs, endMs) else headMs

    /** Sung through to the end of the stretch. */
    val hasFinished: Boolean
        get() = isFinishedAt(positionMs, endMs) || player.hasEnded

    /** Carries on from the head; from the start once the stretch has been heard to its end. */
    fun play() {
        if (isFinishedAt(positionMs, endMs)) seek(startMs)
        player.play()
    }

    fun pause() {
        headMs = positionMs
        player.pause()
    }

    fun seek(positionMs: Long) {
        headMs = positionMs.coerceIn(startMs, endMs)
        player.seekTo(headMs)
    }

    fun unload() {
        isLoaded = false
        player.stop()
    }

    fun release() = player.release()

    companion object {
        /** How near the end counts as there: playback is checked between readings of the clock. */
        private const val END_MARGIN_MS = 20L

        fun isFinishedAt(positionMs: Long, endMs: Long): Boolean = positionMs >= endMs - END_MARGIN_MS
    }
}
