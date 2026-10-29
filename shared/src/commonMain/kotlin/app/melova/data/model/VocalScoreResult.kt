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
    /** Glide/eshareh shape feedback; reuses PitchCorrection for snippet playback. */
    val ornamentCorrections: List<VocalScorer.PitchCorrection> = emptyList(),
    val requiresReRecord: Boolean = false,
    val reRecordReason: ReRecordReason? = null,
    /** Set when the scorer asks for the teacher on this take; the id makes it a free referral. */
    val teacherReferral: TeacherReferral? = null
)

data class TeacherReferral(
    val reasons: List<String>,
    val referralId: String?
)
