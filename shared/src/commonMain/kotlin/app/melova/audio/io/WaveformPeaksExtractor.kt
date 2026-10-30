package app.melova.audio.io

import kotlin.math.abs
import app.melova.platform.LocalFile

object WaveformPeaksExtractor {
    const val DEFAULT_BAR_COUNT = 96

    fun fromWavFile(file: LocalFile, barCount: Int = DEFAULT_BAR_COUNT): List<Float> {
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

    /** Loudest sample of every [ENVELOPE_BIN_MS] of the file, for cutting zoomed windows. */
    fun envelope(file: LocalFile): FloatArray {
        val (samples, sampleRate) = WavUtils.readPcm16Samples(file)
        val samplesPerBin = (sampleRate * ENVELOPE_BIN_MS / 1000).coerceAtLeast(1)
        val binCount = (samples.size + samplesPerBin - 1) / samplesPerBin
        return FloatArray(binCount) { bin ->
            var peak = 0
            val end = minOf((bin + 1) * samplesPerBin, samples.size)
            for (index in bin * samplesPerBin until end) {
                peak = maxOf(peak, abs(samples[index].toInt()))
            }
            peak.toFloat() / Short.MAX_VALUE
        }
    }

    /**
     * [barCount] bars over [startMs]..[endMs] of an [envelope], scaled so the loudest bar
     * of the window is full height: each voice's shape shows whatever its level.
     */
    fun window(envelope: FloatArray, startMs: Long, endMs: Long, barCount: Int): List<Float> {
        if (envelope.isEmpty() || endMs <= startMs || barCount <= 0) return List(barCount.coerceAtLeast(0)) { 0f }
        val firstBin = startMs.toDouble() / ENVELOPE_BIN_MS
        val binsPerBar = (endMs - startMs).toDouble() / ENVELOPE_BIN_MS / barCount
        val bars = List(barCount) { bar ->
            val from = (firstBin + bar * binsPerBar).toInt().coerceIn(0, envelope.size)
            val to = (firstBin + (bar + 1) * binsPerBar).toInt().coerceIn(from + 1, envelope.size + 1)
            var peak = 0f
            for (bin in from until minOf(to, envelope.size)) peak = maxOf(peak, envelope[bin])
            peak
        }
        val loudest = bars.maxOrNull()?.takeIf { it > 0f } ?: return bars
        return bars.map { (it / loudest).coerceIn(0f, 1f) }
    }

    const val ENVELOPE_BIN_MS = 5
}
