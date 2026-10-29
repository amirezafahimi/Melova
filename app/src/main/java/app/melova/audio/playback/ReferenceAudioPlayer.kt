package app.melova.audio.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

class ReferenceAudioPlayer(context: Context) {
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

    val isPlaying: Boolean
        get() = player.isPlaying

    val playbackState: Int
        get() = player.playbackState

    val canResume: Boolean
        get() = !player.isPlaying &&
            player.playbackState == Player.STATE_READY &&
            player.currentPosition > 0L

    fun prepare(
        file: File,
        onFinished: () -> Unit,
        onReady: () -> Unit = {}
    ) = prepare(Uri.fromFile(file), onFinished, onReady)

    /** Same as the file version, for a picked document (content://) or any other Uri. */
    fun prepare(
        uri: Uri,
        onFinished: () -> Unit,
        onReady: () -> Unit = {}
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

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun play() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
        }
        player.playWhenReady = true
        player.play()
    }

    fun playFrom(positionMs: Long) {
        seekTo(positionMs)
        play()
    }

    fun pause() {
        player.playWhenReady = false
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun stop() {
        removeListener()
        setSpeed(1f)
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
    }

    /** سرعت پخش؛ زیر و بمی صدا عوض نمی‌شود تا فالشی همان‌طور که بوده شنیده شود. */
    fun setSpeed(speed: Float) {
        if (player.playbackParameters.speed != speed) {
            player.playbackParameters = PlaybackParameters(speed, 1f)
        }
    }

    /** پخش دوباره‌ی همان فایل از ابتدا تا وقتی خاموش شود (برای تکرار یک بازه). */
    fun setLooping(loop: Boolean) {
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun currentPositionMs(): Long = player.currentPosition.coerceAtLeast(0L)

    fun durationMs(): Long {
        val duration = player.duration
        return if (duration > 0L) duration else 0L
    }

    fun currentProgress(): Float {
        val duration = durationMs()
        return if (duration > 0L) {
            (currentPositionMs().toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    fun release() {
        removeListener()
        player.release()
    }

    private fun removeListener() {
        listener?.let { player.removeListener(it) }
        listener = null
    }
}
