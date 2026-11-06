package app.melova.audio.io

import kotlin.test.Test
import kotlin.test.assertEquals

class TakeMarkerTest {
    @Test
    fun aMarkedTakeStartsWithAQuarterSecondHoldingOneClickAtATenthOfASecond() {
        val take = ShortArray(44_100) { 1_000 }
        val (marked, sampleRate) = WavUtils.readPcm16Le(withTakeMarker(WavUtils.pcm16Wav(take, 44_100)))
        val samples = WavUtils.pcmBytesToSamples(marked)

        assertEquals(44_100, sampleRate)
        assertEquals(11_025 + take.size, samples.size)
        assertEquals(listOf(4_410), samples.take(11_025).indices.filter { samples[it] != 0.toShort() })
        assertEquals(take.toList(), samples.drop(11_025))
    }
}
