package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackClipDto(
    @SerialName("start_ms") val startMs: Long = 0L,
    @SerialName("end_ms") val endMs: Long = 0L,
    @SerialName("slow_start_ms") val slowStartMs: Long = 0L,
    @SerialName("slow_end_ms") val slowEndMs: Long = 0L,
    @SerialName("slow_speed") val slowSpeed: Float = 1f,
    @SerialName("ramp_ms") val rampMs: Long = 0L
)
