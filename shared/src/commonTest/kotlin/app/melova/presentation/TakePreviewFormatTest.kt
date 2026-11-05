package app.melova.presentation

import app.melova.audio.io.WavUtils
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TakePreviewFormatTest {
    @Test
    fun aMono16BitWavIsReadOnTheDevice() {
        assertTrue(TakePreviewViewModel.isMonoPcm16(WavUtils.pcm16Wav(ShortArray(2_000), 44_100)))
    }

    @Test
    fun aStereoWavIsFetchedFromTheServerInstead() {
        val stereo = WavUtils.pcm16Wav(ShortArray(2_000), 44_100).also { it[22] = 2 }
        assertFalse(TakePreviewViewModel.isMonoPcm16(stereo))
    }

    @Test
    fun anMp3IsFetchedFromTheServerInstead() {
        val mp3 = ByteArray(200).also { "ID3".encodeToByteArray().copyInto(it) }
        assertFalse(TakePreviewViewModel.isMonoPcm16(mp3))
    }
}
