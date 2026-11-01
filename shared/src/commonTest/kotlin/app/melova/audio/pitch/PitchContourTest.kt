package app.melova.audio.pitch

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private const val SAMPLE_RATE = 44_100

/** A sung tone at [note], or silence where [note] gives null, from a function of time in ms. */
private fun tone(
    durationMs: Int,
    fundamental: Double = 1.0,
    secondHarmonic: Double = 0.0,
    note: (Long) -> Float?
): ShortArray {
    var phase = 0.0
    return ShortArray(durationMs * SAMPLE_RATE / 1000) { index ->
        val semitone = note(index * 1000L / SAMPLE_RATE) ?: return@ShortArray 0
        phase += 2 * PI * 440.0 * 2.0.pow((semitone - 69.0) / 12.0) / SAMPLE_RATE
        ((fundamental * sin(phase) + secondHarmonic * sin(2 * phase)) * 12_000).toInt().toShort()
    }
}

private fun line(fromMs: Long, toMs: Long, note: Float) =
    (fromMs..toMs step PitchContour.HOP_MS).map { PitchPoint(it, note) }

class PitchContourTest {
    @Test
    fun aSteadyToneReadsAsItsOwnNote() = runTest {
        val line = PitchContour.of(tone(1_000) { 57f }, SAMPLE_RATE, 100, 900)

        assertEquals(1, line.size)
        assertTrue(line.single().all { abs(it.semitone - 57f) < 0.1f })
    }

    @Test
    fun anOpenVowelWithAStrongSecondHarmonicStaysOnItsNote() = runTest {
        // The «آ» of «تمام»: the harmonic won for 270 ms, more than the octave check could see.
        val samples = tone(1_000, fundamental = 0.3, secondHarmonic = 1.0) { 60f }

        val notes = PitchContour.of(samples, SAMPLE_RATE, 100, 900).flatten()

        assertTrue(notes.isNotEmpty() && notes.all { abs(it.semitone - 60f) < 0.2f })
    }

    @Test
    fun aHighNoteWithAHarmonicIsNotPulledAnOctaveDown() = runTest {
        val samples = tone(1_000, fundamental = 1.0, secondHarmonic = 0.5) { 72f }

        val notes = PitchContour.of(samples, SAMPLE_RATE, 100, 900).flatten()

        assertTrue(notes.isNotEmpty() && notes.all { abs(it.semitone - 72f) < 0.2f })
    }

    @Test
    fun aVibratoStaysInTheLine() = runTest {
        val samples = tone(1_000) { ms -> 60f + 0.5f * sin(2 * PI * 6.0 * ms / 1000).toFloat() }

        val notes = PitchContour.of(samples, SAMPLE_RATE, 100, 900).flatten().map { it.semitone }

        assertTrue((notes.maxOrNull() ?: 0f) - (notes.minOrNull() ?: 0f) > 0.7f)
    }

    @Test
    fun silenceDrawsNoLine() = runTest {
        assertTrue(PitchContour.of(ShortArray(SAMPLE_RATE), SAMPLE_RATE, 0, 1_000).isEmpty())
    }

    @Test
    fun aPauseInTheSingingBreaksTheLine() = runTest {
        val samples = tone(1_000) { ms -> if (ms in 400..600) null else 60f }

        val line = PitchContour.of(samples, SAMPLE_RATE, 100, 900)

        assertEquals(2, line.size)
        assertTrue(line[0].last().timeMs < 420 && line[1].first().timeMs > 580)
    }

    @Test
    fun aStudentAnOctaveAboveTheTeacherIsMovedDownToTheTeacher() {
        val teacher = listOf(line(0, 500, 55f))
        val student = listOf(line(0, 500, 67.5f))

        val moved = PitchContour.inRegisterOf(teacher, student).flatten()

        assertTrue(moved.all { it.semitone == 55.5f })
    }

    @Test
    fun theGuideFollowsTheStudentsOwnKey() {
        val teacher = listOf(line(0, 1_000, 60f))
        val student = listOf(line(0, 1_000, 59.4f))

        val guide = PitchContour.guide(teacher, student, 500L..500L) { it }.flatten()

        assertTrue(guide.all { abs(it.semitone - 59.4f) < 0.01f })
    }

    @Test
    fun theGuideStopsFollowingWhereTheScoreStops() {
        val teacher = listOf(line(0, 1_000, 60f))
        val student = listOf(line(0, 1_000, 57f))

        val guide = PitchContour.guide(teacher, student, 500L..500L) { it }.flatten()

        assertTrue(guide.all { abs(it.semitone - 59f) < 0.01f })
    }

    @Test
    fun theMistakeItselfDoesNotPullTheGuideTowardsTheStudent() {
        val teacher = listOf(line(0, 1_000, 60f))
        val student = listOf(line(0, 1_000, 60f).map { point ->
            if (point.timeMs in 300..700) point.copy(semitone = 63.3f) else point
        })

        val guide = PitchContour.guide(teacher, student, 500L..500L) { it }.flatten()

        assertTrue(guide.all { it.semitone == 60f })
    }

    @Test
    fun theGuideRunsOnTheStudentsClock() {
        val teacher = listOf(line(0, 1_000, 60f))

        val guide = PitchContour.guide(teacher, teacher, 500L..500L) { it + 120 }.flatten()

        assertEquals(120L, guide.first().timeMs)
    }

    @Test
    fun theOffStretchEndsWhereTheVoiceComesBackToTheGuide() {
        val guide = line(0, 1_000, 60f)
        val student = listOf(line(0, 1_000, 60f).map { point ->
            if (point.timeMs in 300..500) point.copy(semitone = 63f) else point
        })

        val stretch = PitchContour.offStretch(student, guide, 400)

        assertEquals(300L, stretch.first().first.timeMs)
        assertEquals(500L, stretch.last().first.timeMs)
        assertTrue(stretch.all { it.second == 60f })
    }

    @Test
    fun noStretchIsLitWhereTheVoiceIsOnTheGuide() {
        val guide = line(0, 1_000, 60f)

        assertTrue(PitchContour.offStretch(listOf(guide), guide, 400).isEmpty())
    }

    @Test
    fun bothLanesFitTheRangeWithRoomToSpare() {
        val range = PitchContour.rangeOf(listOf(listOf(line(0, 100, 50f)), listOf(line(0, 100, 70f))))

        assertTrue(range.start < 50f && range.endInclusive > 70f)
    }

    @Test
    fun aHeldNoteDoesNotFillTheWholeCard() {
        val range = PitchContour.rangeOf(listOf(listOf(line(0, 100, 60f))))

        assertTrue(range.endInclusive - range.start >= 8f)
    }
}
