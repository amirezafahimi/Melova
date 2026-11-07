package app.melova.data.model

import app.melova.audio.scoring.VocalScorer
import kotlin.test.Test
import kotlin.test.assertEquals

class MistakeOrderTest {
    private fun note(startMs: Long, cents: Float, sung: String = "لا۴") = VocalScorer.PitchCorrection(
        startMs = startMs,
        endMs = startMs + 300,
        timeLabel = "",
        expectedNote = "لا۴",
        sungNote = sung,
        centsOff = cents,
        direction = if (cents > 0) VocalScorer.CorrectionDirection.SHARP else VocalScorer.CorrectionDirection.FLAT,
        message = ""
    )

    private fun ornament(startMs: Long, score: Int) = TahrirComparison(
        kind = TahrirKind.SUSTAINED,
        refStartMs = startMs,
        refEndMs = startMs + 500,
        studentStartMs = startMs,
        studentEndMs = startMs + 500,
        score = score,
        vowelCount = 1,
        message = ""
    )

    @Test
    fun aNoteFurtherOffComesFirstWhicheverWayItIsOff() {
        val order = listOf(note(0, 60f), note(1_000, -150f), note(2_000, 90f)).worstFirst()
        assertEquals(listOf(1_000L, 2_000L, 0L), order.map { it.startMs })
    }

    @Test
    fun aNoteNotSungCountsAsAWholeToneOff() {
        val order = listOf(note(0, 120f), note(1_000, 0f, sung = "—"), note(2_000, 250f)).worstFirst()
        assertEquals(listOf(2_000L, 1_000L, 0L), order.map { it.startMs })
    }

    @Test
    fun equalMistakesKeepTheOrderTheyWereSungIn() {
        val order = listOf(note(0, 100f), note(1_000, -100f)).worstFirst()
        assertEquals(listOf(0L, 1_000L), order.map { it.startMs })
    }

    @Test
    fun theOrnamentFurthestFromTheTeachersComesFirst() {
        val order = listOf(ornament(0, 80), ornament(1_000, 35), ornament(2_000, 60)).furthestFirst()
        assertEquals(listOf(1_000L, 2_000L, 0L), order.map { it.refStartMs })
    }
}
