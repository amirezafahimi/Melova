package app.melova.audio.io

/** The rate the server's models hear a take at. */
const val SPEECH_SAMPLE_RATE = 16_000

/**
 * A take brought down to [SPEECH_SAMPLE_RATE], a piece at a time, picking the samples the
 * server picks from a 44.1 kHz take (to_mono_16khz in audio_io.py): sample j is the one at
 * j * rate / 16 000, rounded down, with no filter. A filtered resample sounds cleaner but moved
 * one take's score by 11; these picks kept 14 of 18 takes on the same score and the rest within
 * 1 to 3. A rate at or below 16 kHz is kept as it is.
 */
class SpeechRateDecimator(private val sourceRate: Int) {
    private val ratio = sourceRate.toDouble() / SPEECH_SAMPLE_RATE
    private var consumed = 0L
    private var next = 0L

    val outputRate: Int get() = if (sourceRate > SPEECH_SAMPLE_RATE) SPEECH_SAMPLE_RATE else sourceRate

    fun take(samples: ShortArray): ShortArray {
        if (sourceRate <= SPEECH_SAMPLE_RATE) return samples
        val end = consumed + samples.size
        val picked = ShortArray((samples.size / ratio).toInt() + 2)
        var count = 0
        while (true) {
            val source = (next * ratio).toLong()
            if (source >= end) break
            picked[count++] = samples[(source - consumed).toInt()]
            next++
        }
        consumed = end
        return picked.copyOf(count)
    }
}

/** A whole take at [SPEECH_SAMPLE_RATE], and the rate it ends up at. */
fun atSpeechRate(samples: ShortArray, sampleRate: Int): Pair<ShortArray, Int> {
    val decimator = SpeechRateDecimator(sampleRate)
    return decimator.take(samples) to decimator.outputRate
}
