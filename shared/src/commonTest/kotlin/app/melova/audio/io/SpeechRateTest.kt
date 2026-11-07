package app.melova.audio.io

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SpeechRateTest {
    @Test
    fun theSamplesPickedAreTheOnesTheServerPicks() {
        val take = ShortArray(44_100) { it.toShort() }
        val (samples, rate) = atSpeechRate(take, 44_100)

        assertEquals(16_000, rate)
        assertEquals(16_000, samples.size)
        // to_mono_16khz: sample j is the one at j * 2.75625, rounded down.
        assertContentEquals(shortArrayOf(0, 2, 5, 8, 11, 13), samples.copyOf(6))
        assertEquals(44_097.toShort(), samples.last())
    }

    @Test
    fun aTakeInPiecesOfAnySizeComesOutAsTheWholeOne() {
        val take = ShortArray(100_003) { (it * 7 % 30_000).toShort() }
        val decimator = SpeechRateDecimator(44_100)
        var at = 0
        val pieces = mutableListOf<Short>()
        for (size in listOf(1, 3, 4_410, 999, 44_100, 50_490)) {
            pieces += decimator.take(take.copyOfRange(at, at + size)).toList()
            at += size
        }

        assertContentEquals(atSpeechRate(take, 44_100).first, pieces.toShortArray())
    }

    @Test
    fun aTakeAlreadyAtSixteenKilohertzIsKept() {
        val take = ShortArray(500) { it.toShort() }
        val (samples, rate) = atSpeechRate(take, 16_000)

        assertEquals(16_000, rate)
        assertContentEquals(take, samples)
    }
}
