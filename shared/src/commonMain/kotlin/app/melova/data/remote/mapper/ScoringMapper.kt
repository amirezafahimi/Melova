package app.melova.data.remote.mapper

import app.melova.audio.playback.ExpectedSegment
import app.melova.audio.scoring.ReRecordReason
import app.melova.audio.scoring.VocalScorer
import app.melova.data.model.PlaybackClip
import app.melova.data.model.ScoreMetric
import app.melova.data.model.TahrirComparison
import app.melova.data.model.TahrirKind
import app.melova.data.model.TeacherReferral
import app.melova.data.model.VocalScoreResult
import app.melova.data.remote.dto.ExpectedSegmentDto
import app.melova.data.remote.dto.OrnamentCorrectionDto
import app.melova.data.remote.dto.PitchCorrectionDto
import app.melova.data.remote.dto.PlaybackClipDto
import app.melova.data.remote.dto.ScoreMetricDto
import app.melova.data.remote.dto.ScoringResponseDto
import app.melova.data.remote.dto.TahrirComparisonDto

fun ScoringResponseDto.toDomain(): VocalScoreResult {
    val resolvedSummary = if (hasValidInput) {
        summary
    } else {
        failureReason ?: summary.ifBlank { "نمره‌دهی ممکن نبود." }
    }

    return VocalScoreResult(
        overall = overall,
        scoreMetrics = scoreMetrics.toDomainMetrics(pitchAccuracy),
        summary = resolvedSummary,
        technicalAnalysis = technicalAnalysis.ifBlank { failureReason.orEmpty() },
        weakPoints = weakPoints,
        corrections = corrections.map { it.toDomain() },
        tahrirComparisons = tahrirComparisons.map { it.toDomain() },
        ornamentCorrections = ornamentCorrections.map { it.toDomain() },
        requiresReRecord = requiresReRecord,
        reRecordReason = reRecordReason.toDomain(),
        teacherReferral = teacherReview?.takeIf { it.suggested }?.let {
            TeacherReferral(reasons = it.reasons, referralId = it.referralId)
        }
    )
}

fun ScoreMetricDto.toDomain(): ScoreMetric =
    ScoreMetric(
        id = id,
        label = label,
        value = value,
        hint = hint
    )

private fun List<ScoreMetricDto>.toDomainMetrics(pitchAccuracyFallback: Int): List<ScoreMetric> {
    if (isNotEmpty()) return map { it.toDomain() }
    if (pitchAccuracyFallback <= 0) return emptyList()
    return listOf(
        ScoreMetric(
            id = "pitch_accuracy",
            label = "دقت نت",
            value = pitchAccuracyFallback,
            hint = null
        )
    )
}

fun PitchCorrectionDto.toDomain(): VocalScorer.PitchCorrection =
    VocalScorer.PitchCorrection(
        startMs = startMs,
        endMs = endMs,
        timeLabel = timeLabel,
        expectedNote = expectedNote,
        sungNote = sungNote,
        centsOff = centsOff,
        direction = direction.toCorrectionDirection(),
        message = message,
        referenceStartMs = referenceStartMs,
        referenceEndMs = referenceEndMs,
        studentClip = studentClip?.toDomain(),
        teacherClip = teacherClip?.toDomain()
    )

fun OrnamentCorrectionDto.toDomain(): VocalScorer.PitchCorrection =
    VocalScorer.PitchCorrection(
        startMs = startMs,
        endMs = endMs,
        timeLabel = timeLabel,
        expectedNote = "",
        sungNote = "",
        centsOff = 0f,
        direction = VocalScorer.CorrectionDirection.GOOD,
        message = message,
        referenceStartMs = referenceStartMs,
        referenceEndMs = referenceEndMs,
        studentClip = studentClip?.toDomain(),
        teacherClip = teacherClip?.toDomain()
    )

fun TahrirComparisonDto.toDomain(): TahrirComparison =
    TahrirComparison(
        kind = TahrirKind.fromRaw(kind),
        refStartMs = refStartMs,
        refEndMs = refEndMs,
        studentStartMs = studentStartMs,
        studentEndMs = studentEndMs,
        score = score,
        vowelCount = vowelCount,
        message = message,
        studentClip = studentClip?.toDomain(),
        teacherClip = teacherClip?.toDomain()
    )

fun PlaybackClipDto.toDomain(): PlaybackClip? =
    if (endMs > startMs) {
        PlaybackClip(
            startMs = startMs,
            endMs = endMs,
            slowStartMs = slowStartMs,
            slowEndMs = slowEndMs,
            slowSpeed = slowSpeed.coerceIn(0.25f, 1f),
            rampMs = rampMs
        )
    } else {
        null
    }

private fun String.toCorrectionDirection(): VocalScorer.CorrectionDirection =
    when (uppercase()) {
        "SHARP" -> VocalScorer.CorrectionDirection.SHARP
        "FLAT" -> VocalScorer.CorrectionDirection.FLAT
        else -> VocalScorer.CorrectionDirection.GOOD
    }

fun ExpectedSegment.toDto(): ExpectedSegmentDto =
    ExpectedSegmentDto(
        startMs = startMs,
        endMs = endMs,
        frequencyHz = frequencyHz,
        label = label
    )

private fun String?.toDomain(): ReRecordReason? = when (this?.uppercase()) {
    ReRecordReason.UNRELATED_LYRICS.name -> ReRecordReason.UNRELATED_LYRICS
    ReRecordReason.UNRELATED_MELODY.name -> ReRecordReason.UNRELATED_MELODY
    else -> null
}
