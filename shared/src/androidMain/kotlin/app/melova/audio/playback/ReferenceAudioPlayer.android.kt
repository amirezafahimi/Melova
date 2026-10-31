package app.melova.audio.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.melova.platform.LocalFile
import app.melova.platform.javaFile

actual class ReferenceAudioPlayer actual constructor(context: Context) {
    private val player = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        .build()
    private var listener: Player.Listener? = null

    actual val isPlaying: Boolean
        get() = player.isPlaying

    actual val hasEnded: Boolean
        get() = player.playbackState == Player.STATE_ENDED

    actual val canResume: Boolean
        get() = !player.isPlaying &&
            player.playbackState == Player.STATE_READY &&
            player.currentPosition > 0L

    actual fun prepare(
        file: LocalFile,
        onFinished: () -> Unit,
        onReady: () -> Unit
    ) = prepare(Uri.fromFile(file.javaFile), onFinished, onReady)

    private fun prepare(
        uri: Uri,
        onFinished: () -> Unit,
        onReady: () -> Unit
    ) {
        removeListener()
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.setMediaItem(MediaItem.fromUri(uri))
        player.playWhenReady = false
        player.prepare()

        var readyDispatched = false
        listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        if (!readyDispatched) {
                            readyDispatched = true
                            onReady()
                        }
                    }
                    Player.STATE_ENDED -> onFinished()
                }
            }
        }
        player.addListener(listener!!)
    }

    actual fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    actual fun play() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
        }
        player.playWhenReady = true
        player.play()
    }

    actual fun playFrom(positionMs: Long) {
        seekTo(positionMs)
        play()
    }

    actual fun pause() {
        player.playWhenReady = false
        player.pause()
    }

    actual fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    actual fun stop() {
        removeListener()
        setSpeed(1f)
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
    }

    /** Playback speed; the pitch stays, so an off note is heard as it was sung. */
    actual fun setSpeed(speed: Float) {
        if (player.playbackParameters.speed != speed) {
            player.playbackParameters = PlaybackParameters(speed, 1f)
        }
    }

    /** Plays the same file over from the start until turned off, to repeat a segment. */
    actual fun setLooping(loop: Boolean) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    actual fun currentPositionMs(): Long = player.currentPosition.coerceAtLeast(0L)

    actual fun durationMs(): Long {
        val duration = player.duration
        return if (duration > 0L) duration else 0L
    }

    actual fun currentProgress(): Float {
        val duration = durationMs()
        return if (duration > 0L) {
            (currentPositionMs().toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    actual fun release() {
        removeListener()
        player.release()
    }

    private fun removeListener() {
        listener?.let { player.removeListener(it) }
        listener = null
    }
}
