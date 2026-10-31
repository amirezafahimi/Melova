package app.melova.audio.playback

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DetailVoicePlayerTest {
    @Test
    fun aHeadInTheMiddleIsNotFinished() {
        assertFalse(DetailVoicePlayer.isFinishedAt(positionMs = 900, endMs = 2_000))
    }

    @Test
    fun aHeadAtTheEndIsFinished() {
        assertTrue(DetailVoicePlayer.isFinishedAt(positionMs = 1_985, endMs = 2_000))
    }
}
