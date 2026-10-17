package app.melova.data.model

import app.melova.audio.scoring.ReRecordReason
import app.melova.audio.scoring.VocalScorer

data class VocalScoreResult(
    val overall: Int,
    val scoreMetrics: List<ScoreMetric> = emptyList(),
    val summary: String,
    val technicalAnalysis: String,
    val weakPoints: List<String>,
    val corrections: List<VocalScorer.PitchCorrection> = emptyList(),
    val tahrirComparisons: List<TahrirComparison> = emptyList(),
    val requiresReRecord: Boolean = false,
    val reRecordReason: ReRecordReason? = null
)
