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
    @SerializedName("corrections") val corrections: List<PitchCorrectionDto> = emptyList()
)
