package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrnamentCorrectionDto(
    @SerialName("start_ms") val startMs: Long,
    @SerialName("end_ms") val endMs: Long,
    @SerialName("time_label") val timeLabel: String,
    @SerialName("kind") val kind: String,
    @SerialName("message") val message: String,
    @SerialName("reference_start_ms") val referenceStartMs: Long = 0L,
    @SerialName("reference_end_ms") val referenceEndMs: Long? = null,
    @SerialName("student_clip") val studentClip: PlaybackClipDto? = null,
    @SerialName("teacher_clip") val teacherClip: PlaybackClipDto? = null
)
