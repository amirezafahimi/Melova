package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PitchCorrectionDto(
    @SerialName("start_ms") val startMs: Long,
    @SerialName("end_ms") val endMs: Long,
    @SerialName("time_label") val timeLabel: String,
    @SerialName("expected_note") val expectedNote: String,
    @SerialName("sung_note") val sungNote: String,
    @SerialName("cents_off") val centsOff: Float,
    @SerialName("direction") val direction: String,
    @SerialName("message") val message: String,
    @SerialName("reference_start_ms") val referenceStartMs: Long = 0L,
    @SerialName("reference_end_ms") val referenceEndMs: Long? = null,
    @SerialName("student_clip") val studentClip: PlaybackClipDto? = null,
    @SerialName("teacher_clip") val teacherClip: PlaybackClipDto? = null,
    @SerialName("word") val word: String? = null
)
