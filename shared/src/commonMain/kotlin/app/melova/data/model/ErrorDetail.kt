package app.melova.data.model

import app.melova.audio.scoring.VocalScorer

/**
 * «دیدن جزییات» for one mistake: both waves around it, with the moment of a note mistake
 * (a point) or the span of a tahrir or vibrato marked on them.
 */
data class ErrorDetail(
    /** Same key the rows use for the snippet that is playing. */
    val key: Long,
    val markKind: ErrorMarkKind,
    val teacher: ErrorDetailLane?,
    val student: ErrorDetailLane?,
    val correction: VocalScorer.PitchCorrection? = null,
    val comparison: TahrirComparison? = null
)

enum class ErrorMarkKind {
    /** A wrong note: one moment. */
    MOMENT,

    /** A tahrir or vibrato: a stretch of time. */
    SPAN
}

/** One voice around the error; all times are in that recording's own file. */
data class ErrorDetailLane(
    val startMs: Long,
    val endMs: Long,
    val peaks: List<Float>,
    val markStartMs: Long,
    val markEndMs: Long
)
