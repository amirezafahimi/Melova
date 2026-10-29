package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OrnamentCorrectionDto(
    @SerializedName("start_ms") val startMs: Long,
    @SerializedName("end_ms") val endMs: Long,
    @SerializedName("time_label") val timeLabel: String,
    @SerializedName("kind") val kind: String,
    @SerializedName("message") val message: String,
    @SerializedName("reference_start_ms") val referenceStartMs: Long = 0L,
    @SerializedName("reference_end_ms") val referenceEndMs: Long? = null,
    @SerializedName("student_clip") val studentClip: PlaybackClipDto? = null,
    @SerializedName("teacher_clip") val teacherClip: PlaybackClipDto? = null
)
