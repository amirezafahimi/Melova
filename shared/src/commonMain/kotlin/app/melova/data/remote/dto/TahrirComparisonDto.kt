package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TahrirComparisonDto(
    @SerialName("kind") val kind: String = "",
    @SerialName("ref_start_ms") val refStartMs: Long = 0L,
    @SerialName("ref_end_ms") val refEndMs: Long = 0L,
    @SerialName("student_start_ms") val studentStartMs: Long? = null,
    @SerialName("student_end_ms") val studentEndMs: Long? = null,
    @SerialName("average_error_semitones") val averageErrorSemitones: Float = 0f,
    @SerialName("score") val score: Int = 0,
    @SerialName("rhythm_error_ratio") val rhythmErrorRatio: Float? = null,
    @SerialName("vowel_count") val vowelCount: Int = 0,
    @SerialName("message") val message: String = "",
    @SerialName("student_clip") val studentClip: PlaybackClipDto? = null,
    @SerialName("teacher_clip") val teacherClip: PlaybackClipDto? = null
)
