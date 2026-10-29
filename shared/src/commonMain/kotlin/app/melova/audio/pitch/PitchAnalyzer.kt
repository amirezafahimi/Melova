package app.melova.audio.pitch

import app.melova.audio.io.WavUtils
import app.melova.platform.LocalFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PitchAnalyzer {
    data class PitchFrame(
        val timeMs: Long,
        val frequencyHz: Float,
        val probability: Float
    )

    suspend fun analyzeFile(file: LocalFile, sampleRate: Int = WavUtils.SAMPLE_RATE): List<PitchFrame> =
        withContext(Dispatchers.Default) {
            val (samples, detectedSampleRate) = WavUtils.readPcm16Samples(file)
            val effectiveSampleRate = if (detectedSampleRate > 0) detectedSampleRate else sampleRate

            // Back-to-back buffers with no overlap, the last one zero-padded, as TarsosDSP did.
            val yin = Yin(effectiveSampleRate.toFloat(), BUFFER_SIZE)
            val buffer = FloatArray(BUFFER_SIZE)
            val frames = mutableListOf<PitchFrame>()
            var start = 0
            while (start < samples.size) {
                for (index in 0 until BUFFER_SIZE) {
                    val sample = start + index
                    buffer[index] = if (sample < samples.size) samples[sample] / 32768f else 0f
                }
                val (pitch, probability) = yin.detect(buffer)
                if (pitch > 0f) {
                    frames += PitchFrame(
                        timeMs = start * 1000L / effectiveSampleRate,
                        frequencyHz = pitch,
                        probability = probability
                    )
                }
                start += BUFFER_SIZE
            }
            frames
        }

    companion object {
        private const val BUFFER_SIZE = 2048

        fun frequencyToNoteName(frequencyHz: Float): String {
            if (frequencyHz <= 0f) return "—"
            val noteNames = arrayOf("دو", "دو#", "ر", "ر#", "می", "فا", "فا#", "سل", "سل#", "لا", "لا#", "سی")
            val midi = 69 + 12 * (kotlin.math.ln(frequencyHz / 440.0) / kotlin.math.ln(2.0))
            val rounded = midi.toInt().coerceIn(0, 127)
            val noteIndex = ((rounded % 12) + 12) % 12
            val octave = (rounded / 12) - 1
            return "${noteNames[noteIndex]}$octave"
        }

        fun centsDifference(sungHz: Float, targetHz: Float): Float {
            if (sungHz <= 0f || targetHz <= 0f) return 0f
            return 1200f * (kotlin.math.ln(sungHz / targetHz) / kotlin.math.ln(2.0)).toFloat()
        }
    }
}

/**
 * The YIN pitch estimator (de Cheveigné & Kawahara 2002) as TarsosDSP implements it:
 * threshold 0.20, parabolic interpolation, probability 1 − d'(τ).
 */
private class Yin(private val sampleRate: Float, bufferSize: Int) {
    private val yinBuffer = FloatArray(bufferSize / 2)

    /** Pitch in Hz (−1 when unpitched) and its probability. */
    fun detect(audio: FloatArray): Pair<Float, Float> {
        difference(audio)
        cumulativeMeanNormalizedDifference()
        val tau = absoluteThreshold()
        if (tau == -1) return -1f to 0f
        val probability = 1f - yinBuffer[tau]
        val betterTau = parabolicInterpolation(tau)
        return sampleRate / betterTau to probability
    }

    private fun difference(audio: FloatArray) {
        yinBuffer.fill(0f)
        for (tau in 1 until yinBuffer.size) {
            var sum = 0f
            for (index in 0 until yinBuffer.size) {
                val delta = audio[index] - audio[index + tau]
                sum += delta * delta
            }
            yinBuffer[tau] = sum
        }
    }

    private fun cumulativeMeanNormalizedDifference() {
        yinBuffer[0] = 1f
        var runningSum = 0f
        for (tau in 1 until yinBuffer.size) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] *= tau / runningSum
        }
    }

    private fun absoluteThreshold(): Int {
        var tau = 2
        while (tau < yinBuffer.size) {
            if (yinBuffer[tau] < THRESHOLD) {
                while (tau + 1 < yinBuffer.size && yinBuffer[tau + 1] < yinBuffer[tau]) tau++
                return tau
            }
            tau++
        }
        return -1
    }

    private fun parabolicInterpolation(tauEstimate: Int): Float {
        val x0 = if (tauEstimate < 1) tauEstimate else tauEstimate - 1
        val x2 = if (tauEstimate + 1 < yinBuffer.size) tauEstimate + 1 else tauEstimate
        return when {
            x0 == tauEstimate -> if (yinBuffer[tauEstimate] <= yinBuffer[x2]) tauEstimate.toFloat() else x2.toFloat()
            x2 == tauEstimate -> if (yinBuffer[tauEstimate] <= yinBuffer[x0]) tauEstimate.toFloat() else x0.toFloat()
            else -> {
                val s0 = yinBuffer[x0]
                val s1 = yinBuffer[tauEstimate]
                val s2 = yinBuffer[x2]
                tauEstimate + (s2 - s0) / (2 * (2 * s1 - s2 - s0))
            }
        }
    }

    private companion object {
        const val THRESHOLD = 0.20f
    }
}
