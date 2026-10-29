package app.melova.audio.playback

import app.melova.audio.web.MelovaAudioJs
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import app.melova.platform.toInt8Array

actual class ReferenceAudioPlayer actual constructor(context: PlatformContext) {
    private val player = MelovaAudioJs.createPlayer()

    actual val isPlaying: Boolean
        get() = player.isPlaying()

    actual val hasEnded: Boolean
        get() = player.hasEnded()

    actual val canResume: Boolean
        get() = !player.isPlaying() && player.isReady() && player.positionMs() > 0.0

    actual fun prepare(file: LocalFile, onFinished: () -> Unit, onReady: () -> Unit) {
        player.load(file.readBytes().toInt8Array(), file.name, onFinished, onReady)
    }

    actual fun seekTo(positionMs: Long) = player.seek(positionMs.coerceAtLeast(0L).toDouble())

    actual fun play() = player.play()

    actual fun playFrom(positionMs: Long) {
        seekTo(positionMs)
        play()
    }

    actual fun pause() = player.pause()

    actual fun togglePlayPause() {
        if (player.isPlaying()) pause() else play()
    }

    actual fun stop() {
        setSpeed(1f)
        player.stop()
    }

    actual fun setSpeed(speed: Float) = player.setSpeed(speed.toDouble())

    actual fun setLooping(loop: Boolean) = player.setLoop(loop)

    actual fun currentPositionMs(): Long = player.positionMs().toLong().coerceAtLeast(0L)

    actual fun durationMs(): Long = player.durationMs().toLong().coerceAtLeast(0L)

    actual fun currentProgress(): Float {
        val duration = durationMs()
        return if (duration > 0L) (currentPositionMs().toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    actual fun release() = player.stop()
}
