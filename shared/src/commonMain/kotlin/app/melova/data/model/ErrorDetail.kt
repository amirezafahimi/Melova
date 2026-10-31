package app.melova.data.model

import app.melova.audio.pitch.PitchContour
import app.melova.audio.pitch.PitchPoint
import app.melova.audio.scoring.VocalScorer

/**
 * «دیدن جزییات» for one mistake: both voices' pitch lines around it, with the moment of a note
 * mistake (a point) or the span of a tahrir or vibrato marked on them.
 */
data class ErrorDetail(
    /** Same key the rows use for the snippet that is playing. */
    val key: Long,
    val markKind: ErrorMarkKind,
    val teacher: ErrorDetailLane?,
    val student: ErrorDetailLane?,
    val correction: VocalScorer.PitchCorrection? = null,
    val comparison: TahrirComparison? = null,
    /** The notes both lanes share, so the same height is the same note in each. */
    val pitchRange: ClosedFloatingPointRange<Float> = PitchContour.DEFAULT_RANGE
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
    val markStartMs: Long,
    val markEndMs: Long,
    val speed: Float,
    /** Empty until read, a moment after the sheet opens. */
    val pitch: List<List<PitchPoint>> = emptyList(),
    /** On the student's lane: the teacher's line on the student's clock, to sing against. */
    val guide: List<List<PitchPoint>> = emptyList()
) {
    /** The moment of [other] that matches [timeMs] here: both lanes' ends and marks fall together. */
    fun timeOn(other: ErrorDetailLane, timeMs: Long): Long {
        val anchors = listOf(
            startMs to other.startMs,
            markStartMs to other.markStartMs,
            markEndMs to other.markEndMs,
            endMs to other.endMs
        ).sortedBy { it.first }
        val before = anchors.lastOrNull { it.first <= timeMs } ?: anchors.first()
        val after = anchors.firstOrNull { it.first >= timeMs } ?: anchors.last()
        if (before.first == after.first) return after.second + (timeMs - after.first)
        return before.second + (timeMs - before.first) * (after.second - before.second) / (after.first - before.first)
    }
}
