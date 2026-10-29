package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PlaybackClipDto(
    @SerializedName("start_ms") val startMs: Long = 0L,
    @SerializedName("end_ms") val endMs: Long = 0L,
    @SerializedName("slow_start_ms") val slowStartMs: Long = 0L,
    @SerializedName("slow_end_ms") val slowEndMs: Long = 0L,
    @SerializedName("slow_speed") val slowSpeed: Float = 1f,
    @SerializedName("ramp_ms") val rampMs: Long = 0L
)
