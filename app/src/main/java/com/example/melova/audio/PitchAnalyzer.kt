package com.example.melova.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File

class PitchAnalyzer {
    data class PitchFrame(
        val timeMs: Long,
        val frequencyHz: Float,
        val probability: Float
    )

    suspend fun analyzeFile(file: File, sampleRate: Int = WavUtils.SAMPLE_RATE): List<PitchFrame> =
        withContext(Dispatchers.Default) {
            val (pcmData, detectedSampleRate) = WavUtils.readPcm16Le(file)
            val effectiveSampleRate = if (detectedSampleRate > 0) detectedSampleRate else sampleRate

            val frames = mutableListOf<PitchFrame>()
            val format = TarsosDSPAudioFormat(
                effectiveSampleRate.toFloat(),
                16,
                1,
                true,
                false
            )
            val inputStream = UniversalAudioInputStream(ByteArrayInputStream(pcmData), format)
            val dispatcher = AudioDispatcher(inputStream, BUFFER_SIZE, 0)
            val handler = PitchDetectionHandler { result, event ->
                val pitch = result.pitch
                if (result.isPitched && pitch > 0f) {
                    frames += PitchFrame(
                        timeMs = (event.timeStamp * 1000).toLong(),
                        frequencyHz = pitch,
                        probability = result.probability
                    )
                }
            }
            val processor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN,
                effectiveSampleRate.toFloat(),
                BUFFER_SIZE,
                handler
            )
            dispatcher.addAudioProcessor(processor)
            dispatcher.run()
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
