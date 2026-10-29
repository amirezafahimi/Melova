package app.melova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ScoringResponseDto(
    @SerializedName("has_valid_input") val hasValidInput: Boolean = true,
    @SerializedName("failure_reason") val failureReason: String? = null,
    @SerializedName("requires_re_record") val requiresReRecord: Boolean = false,
    @SerializedName("re_record_reason") val reRecordReason: String? = null,
    @SerializedName("overall") val overall: Int = 0,
    @SerializedName("pitch_accuracy") val pitchAccuracy: Int = 0,
    @SerializedName("score_metrics") val scoreMetrics: List<ScoreMetricDto> = emptyList(),
    @SerializedName("summary") val summary: String = "",
    @SerializedName("technical_analysis") val technicalAnalysis: String = "",
    @SerializedName("weak_points") val weakPoints: List<String> = emptyList(),
    @SerializedName("corrections") val corrections: List<PitchCorrectionDto> = emptyList(),
    @SerializedName("tahrir_comparisons") val tahrirComparisons: List<TahrirComparisonDto> = emptyList(),
    @SerializedName("ornament_corrections") val ornamentCorrections: List<OrnamentCorrectionDto> = emptyList(),
    @SerializedName("teacher_review") val teacherReview: TeacherReviewSuggestionDto? = null
)

/** The scorer wasn't sure of this take and suggests sending it to the teacher, free. */
data class TeacherReviewSuggestionDto(
    @SerializedName("suggested") val suggested: Boolean = false,
    @SerializedName("reasons") val reasons: List<String> = emptyList(),
    @SerializedName("referral_id") val referralId: String? = null
)
