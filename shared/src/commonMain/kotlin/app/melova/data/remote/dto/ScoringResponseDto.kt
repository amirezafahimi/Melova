package app.melova.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScoringResponseDto(
    @SerialName("has_valid_input") val hasValidInput: Boolean = true,
    @SerialName("failure_reason") val failureReason: String? = null,
    @SerialName("requires_re_record") val requiresReRecord: Boolean = false,
    @SerialName("re_record_reason") val reRecordReason: String? = null,
    @SerialName("overall") val overall: Int = 0,
    @SerialName("pitch_accuracy") val pitchAccuracy: Int = 0,
    @SerialName("score_metrics") val scoreMetrics: List<ScoreMetricDto> = emptyList(),
    @SerialName("summary") val summary: String = "",
    @SerialName("technical_analysis") val technicalAnalysis: String = "",
    @SerialName("weak_points") val weakPoints: List<String> = emptyList(),
    @SerialName("corrections") val corrections: List<PitchCorrectionDto> = emptyList(),
    @SerialName("tahrir_comparisons") val tahrirComparisons: List<TahrirComparisonDto> = emptyList(),
    @SerialName("ornament_corrections") val ornamentCorrections: List<OrnamentCorrectionDto> = emptyList(),
    @SerialName("teacher_review") val teacherReview: TeacherReviewSuggestionDto? = null
)

/** The scorer wasn't sure of this take and suggests sending it to the teacher, free. */
@Serializable
data class TeacherReviewSuggestionDto(
    @SerialName("suggested") val suggested: Boolean = false,
    @SerialName("reasons") val reasons: List<String> = emptyList(),
    @SerialName("referral_id") val referralId: String? = null
)
