package app.melova.audio.scoring

import app.melova.audio.pitch.PitchAnalyzer
import app.melova.audio.pitch.PitchAnalyzer.Companion.centsDifference
import app.melova.audio.pitch.PitchAnalyzer.Companion.frequencyToNoteName
import app.melova.audio.playback.ExpectedSegment
import app.melova.data.model.PlaybackClip
import kotlin.math.abs
import kotlin.math.roundToInt
import app.melova.platform.pad2

class VocalScorer {
    data class VocalScoreResult(
        val pitchAccuracy: Int,
        val timing: Int,
        val stability: Int,
        val expression: Int,
        val overall: Int,
        val corrections: List<PitchCorrection>,
        val summary: String
    )

    data class PitchCorrection(
        val startMs: Long,
        val endMs: Long,
        val timeLabel: String,
        val expectedNote: String,
        val sungNote: String,
        val centsOff: Float,
        val direction: CorrectionDirection,
        val message: String,
        val referenceStartMs: Long = startMs,
        val referenceEndMs: Long? = null,
        /** The stretch of this mistake that plays slowly, in the student's and the teacher's voice. */
        val studentClip: PlaybackClip? = null,
        val teacherClip: PlaybackClip? = null
    )

    enum class CorrectionDirection {
        SHARP,
        FLAT,
        GOOD
    }

    fun score(
        expectedSegments: List<ExpectedSegment>,
        sungFrames: List<PitchAnalyzer.PitchFrame>
    ): VocalScoreResult {
        if (sungFrames.isEmpty()) {
            return VocalScoreResult(
                pitchAccuracy = 0,
                timing = 0,
                stability = 0,
                expression = 0,
                overall = 0,
                corrections = emptyList(),
                summary = "صدایی شنیده نشد. میکروفون را بررسی کنید و دوباره تلاش کنید."
            )
        }

        val segmentResults = expectedSegments.map { segment ->
            val framesInSegment = sungFrames.filter { frame ->
                frame.timeMs in segment.startMs..segment.endMs
            }
            val voicedFrames = framesInSegment.filter { it.frequencyHz > 0f }
            val sungFrequency = if (voicedFrames.isNotEmpty()) {
                voicedFrames.map { it.frequencyHz }.average().toFloat()
            } else {
                0f
            }
            val onsetDelayMs = voicedFrames.firstOrNull()?.timeMs?.minus(segment.startMs) ?: Long.MAX_VALUE
            val jitter = if (voicedFrames.size >= 2) {
                val mean = sungFrequency
                voicedFrames.map { abs(it.frequencyHz - mean) }.average().toFloat()
            } else {
                0f
            }

            SegmentEvaluation(
                segment = segment,
                sungFrequencyHz = sungFrequency,
                onsetDelayMs = onsetDelayMs,
                jitterHz = jitter
            )
        }

        val pitchDeviations = segmentResults
            .filter { it.sungFrequencyHz > 0f }
            .map { centsDifference(it.sungFrequencyHz, it.segment.frequencyHz) }

        val pitchAccuracy = scoreFromAverageDeviation(
            average = pitchDeviations.map { abs(it) }.average().toFloat()
        )

        val timingDelays = segmentResults
            .filter { it.sungFrequencyHz > 0f && it.onsetDelayMs != Long.MAX_VALUE }
            .map { it.onsetDelayMs.toFloat() }

        val timing = if (timingDelays.isEmpty()) {
            0
        } else {
            scoreFromAverageDeviation(average = timingDelays.map { abs(it - 120f) }.average().toFloat() / 4f)
        }

        val stability = if (segmentResults.isEmpty()) {
            0
        } else {
            val avgJitter = segmentResults
                .filter { it.sungFrequencyHz > 0f }
                .map { it.jitterHz }
                .average()
                .toFloat()
            scoreFromAverageDeviation(average = avgJitter * 2.5f)
        }

        val voicedRatio = segmentResults.count { it.sungFrequencyHz > 0f }.toFloat() / segmentResults.size
        val expression = (voicedRatio * 100f).roundToInt().coerceIn(0, 100)

        val corrections = segmentResults.mapNotNull { result ->
            if (result.sungFrequencyHz <= 0f) {
                PitchCorrection(
                    startMs = result.segment.startMs,
                    endMs = result.segment.endMs,
                    timeLabel = formatTime(result.segment.startMs),
                    expectedNote = result.segment.label,
                    sungNote = "—",
                    centsOff = 0f,
                    direction = CorrectionDirection.FLAT,
                    message = "در ${formatTimeRange(result.segment.startMs, result.segment.endMs)} — این نت را نخواندید"
                )
            } else {
                val cents = centsDifference(result.sungFrequencyHz, result.segment.frequencyHz)
                val direction = when {
                    abs(cents) <= 35f -> CorrectionDirection.GOOD
                    cents > 0 -> CorrectionDirection.SHARP
                    else -> CorrectionDirection.FLAT
                }
                if (direction == CorrectionDirection.GOOD) null
                else {
                    val timeRange = formatTimeRange(result.segment.startMs, result.segment.endMs)
                    PitchCorrection(
                        startMs = result.segment.startMs,
                        endMs = result.segment.endMs,
                        timeLabel = formatTime(result.segment.startMs),
                        expectedNote = result.segment.label,
                        sungNote = frequencyToNoteName(result.sungFrequencyHz),
                        centsOff = cents,
                        direction = direction,
                        message = when (direction) {
                            CorrectionDirection.SHARP,
                            CorrectionDirection.FLAT -> IntervalLabel.directionMessage(timeRange, cents)
                            CorrectionDirection.GOOD -> "عالی"
                        }
                    )
                }
            }
        }.take(8)

        val overall = listOf(pitchAccuracy, timing, stability, expression)
            .average()
            .roundToInt()
            .coerceIn(0, 100)

        val summary = when {
            overall >= 85 -> "آفرین! آواز شما با نمونه هماهنگ است."
            overall >= 65 -> "خوب بود. با تمرین روی نت‌های اشاره‌شده بهتر می‌شود."
            else -> "نیاز به تمرین بیشتر دارید. ابتدا با دکمه «فقط شنیدن» آشنا شوید."
        }

        return VocalScoreResult(
            pitchAccuracy = pitchAccuracy,
            timing = timing,
            stability = stability,
            expression = expression,
            overall = overall,
            corrections = corrections,
            summary = summary
        )
    }

    private data class SegmentEvaluation(
        val segment: ExpectedSegment,
        val sungFrequencyHz: Float,
        val onsetDelayMs: Long,
        val jitterHz: Float
    )

    private fun scoreFromAverageDeviation(average: Float): Int {
        val score = 100f - average / 2
        return score.roundToInt().coerceIn(0, 100)
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.pad2()}"
    }

    private fun formatTimeRange(startMs: Long, endMs: Long): String {
        val start = formatTime(startMs)
        if (endMs - startMs <= 150L) return start
        return "$start تا ${formatTime(endMs)}"
    }
}
