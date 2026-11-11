package app.melova.data.model

import app.melova.audio.pitch.PitchPoint
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
    /** What went wrong in a rejected take, in a few words; null from older results. */
    val reRecordTitle: String? = null,
    /** Set when the scorer asks for the teacher on this take; the id makes it a free referral. */
    val teacherReferral: TeacherReferral? = null,
    /** Both voices' pitch as the score read it; null on takes scored before it was sent. */
    val pitchLines: ScoredPitchLines? = null,
    /** The section's best score after this take, and whether this take set it; null from older results. */
    val bestScore: Int? = null,
    val isNewBest: Boolean = false,
    /** The server's answer as JSON; sent with a take, it opens this same result again. */
    val sourceJson: String? = null
)

/** Both voices' pitch as the score read them, each on its own recording's clock. */
data class ScoredPitchLines(
    val teacher: List<PitchPoint> = emptyList(),
    val student: List<PitchPoint> = emptyList()
)

data class TeacherReferral(
    val reasons: List<String>,
    val referralId: String?
)
