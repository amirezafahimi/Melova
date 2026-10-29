package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TahrirComparisonDto(
    @SerializedName("kind") val kind: String = "",
    @SerializedName("ref_start_ms") val refStartMs: Long = 0L,
    @SerializedName("ref_end_ms") val refEndMs: Long = 0L,
    @SerializedName("student_start_ms") val studentStartMs: Long? = null,
    @SerializedName("student_end_ms") val studentEndMs: Long? = null,
    @SerializedName("average_error_semitones") val averageErrorSemitones: Float = 0f,
    @SerializedName("score") val score: Int = 0,
    @SerializedName("rhythm_error_ratio") val rhythmErrorRatio: Float? = null,
    @SerializedName("vowel_count") val vowelCount: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("student_clip") val studentClip: PlaybackClipDto? = null,
    @SerializedName("teacher_clip") val teacherClip: PlaybackClipDto? = null
)
