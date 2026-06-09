package app.melova.audio.io

import java.io.File
import kotlin.math.abs

object WaveformPeaksExtractor {
    const val DEFAULT_BAR_COUNT = 96

    fun fromWavFile(file: File, barCount: Int = DEFAULT_BAR_COUNT): List<Float> {
        val (samples, _) = WavUtils.readPcm16Samples(file)
        if (samples.isEmpty() || barCount <= 0) {
            return List(barCount.coerceAtLeast(1)) { 0f }
        }

        val samplesPerBar = (samples.size / barCount).coerceAtLeast(1)
        return List(barCount) { barIndex ->
            val start = barIndex * samplesPerBar
            val end = minOf(start + samplesPerBar, samples.size)
            var peak = 0
            for (index in start until end) {
                peak = maxOf(peak, abs(samples[index].toInt()))
            }
            (peak.toFloat() / Short.MAX_VALUE).coerceIn(0f, 1f)
        }
    }
}
