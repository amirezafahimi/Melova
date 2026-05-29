package com.example.melova.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

class ReferenceAudioPlayer(context: Context) {
    private val player = ExoPlayer.Builder(context).build()

    fun play(file: File, onProgress: (Float) -> Unit, onFinished: () -> Unit) {
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
        player.prepare()
        player.play()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && player.playbackState == Player.STATE_ENDED) {
                    onProgress(1f)
                    onFinished()
                }
            }
        })

        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: androidx.media3.common.Player.Events) {
                val duration = player.duration
                if (duration > 0) {
                    onProgress(player.currentPosition.toFloat() / duration.toFloat())
                }
            }
        })
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
    }

    fun release() {
        player.release()
    }
}
