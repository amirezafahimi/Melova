package app.melova.data.model

import kotlin.math.PI
import kotlin.math.cos

/**
 * The stretch of a mistake to play: [startMs] to [endMs], with the mistake itself
 * ([slowStartMs] to [slowEndMs]) at [slowSpeed]. Speed eases down over the [rampMs] before the
 * mistake and back up after it.
 */
data class PlaybackClip(
    val startMs: Long,
    val endMs: Long,
    val slowStartMs: Long,
    val slowEndMs: Long,
    val slowSpeed: Float,
    val rampMs: Long
) {
    /** The playback speed at [positionMs] of the file. */
    fun speedAt(positionMs: Long): Float {
        val ramp = rampMs.coerceAtLeast(1L)
        val fade = when {
            positionMs >= slowStartMs && positionMs <= slowEndMs -> 1f
            positionMs < slowStartMs -> 1f - (slowStartMs - positionMs).toFloat() / ramp
            else -> 1f - (positionMs - slowEndMs).toFloat() / ramp
        }.coerceIn(0f, 1f)
        val eased = ((1.0 - cos(PI * fade)) / 2.0).toFloat()
        return 1f + (slowSpeed - 1f) * eased
    }
}
