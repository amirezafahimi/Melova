package app.melova.data.local

import android.content.Context
import app.melova.audio.io.AudioFileDecoder
import app.melova.audio.io.WavUtils
import java.io.File
import kotlin.math.abs

/** Local paths for the teacher reference WAV downloaded from the Melova backend. */
object TeacherReferenceCache {
    const val WAV_NAME = "teacher_vocal_reference.wav"
    const val META_NAME = "teacher_vocal_reference.meta"
    const val BACKEND_SOURCE_LABEL = "نمونه استاد (سرور)"
    const val BACKEND_META = "backend:teacher_vocal_demo.wav:peak-0.92"

    fun wavFile(context: Context): File = File(context.filesDir, WAV_NAME)

    fun metaFile(context: Context): File = File(context.filesDir, META_NAME)

    fun syllablesFile(context: Context): File =
        File(context.filesDir, "${wavFile(context).nameWithoutExtension}_syllables.json")

    fun isValidWav(file: File): Boolean =
        file.exists() && file.length() > 44L && AudioFileDecoder.isPcmWav(file)

    fun isReady(context: Context): Boolean = isValidWav(wavFile(context))

    /** Maps internal cache meta to a short user-facing label (never expose raw fingerprints). */
    fun displaySourceLabel(meta: String): String = when {
        meta.startsWith("backend:") -> BACKEND_SOURCE_LABEL
        meta.startsWith("teacher_vocal_demo.wav") -> BACKEND_SOURCE_LABEL
        else -> BACKEND_SOURCE_LABEL
    }

    fun normalizeMetaIfNeeded(context: Context) {
        val metaFile = metaFile(context)
        val meta = metaFile.takeIf { it.exists() }?.readText()?.trim().orEmpty()
        if (meta != BACKEND_META) {
            normalizePlaybackWav(context)
            metaFile.writeText(BACKEND_META)
        }
    }

    /** Boost quiet backend WAV to a consistent playback level for ExoPlayer/speaker output. */
    fun normalizePlaybackWav(context: Context) {
        val wav = wavFile(context)
        if (!isValidWav(wav)) return
        val (samples, sampleRate) = WavUtils.readPcm16Samples(wav)
        if (samples.isEmpty()) return
        val peak = samples.maxOf { abs(it.toInt()) }
        if (peak <= 0) return
        val normalized = WavUtils.normalizePeak(samples)
        WavUtils.writePcm16Wav(wav, normalized, sampleRate)
    }
}
