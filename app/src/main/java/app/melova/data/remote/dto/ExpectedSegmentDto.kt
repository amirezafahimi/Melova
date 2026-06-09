package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExpectedSegmentDto(
    @SerializedName("start_ms") val startMs: Long,
    @SerializedName("end_ms") val endMs: Long,
    @SerializedName("frequency_hz") val frequencyHz: Float,
    @SerializedName("label") val label: String
)
