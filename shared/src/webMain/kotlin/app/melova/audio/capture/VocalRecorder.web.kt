package app.melova.audio.capture

import app.melova.audio.io.WavUtils
import app.melova.audio.web.JsTake
import app.melova.audio.web.MelovaAudioJs
import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext
import app.melova.platform.toByteArray
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlin.math.abs

actual class VocalRecorder actual constructor(context: PlatformContext) {
    private var take: JsTake? = null
    private var stopRequested = false

    actual fun requestStop() {
        stopRequested = true
    }

    actual fun level(): Float = take?.level()?.toFloat() ?: 0f

    actual suspend fun recordFor(durationMs: Long, outputFile: LocalFile): Long =
        record(outputFile, durationMs, stopWhenRequested = false)

    actual suspend fun recordFreeform(
        outputFile: LocalFile,
        maxDurationMs: Long,
        onSamples: ((ShortArray, Int) -> Unit)?
    ): Long = record(outputFile, maxDurationMs, stopWhenRequested = true, onSamples = onSamples)

    private suspend fun record(
        outputFile: LocalFile,
        maxDurationMs: Long,
        stopWhenRequested: Boolean,
        onSamples: ((ShortArray, Int) -> Unit)? = null
    ): Long {
        stopRequested = false
        val active = browserAudio { MelovaAudioJs.startCapture(WavUtils.SAMPLE_RATE).await<JsAny?>()!!.unsafeCast<JsTake>() }
        take = active
        var handedOver = 0
        // The take keeps at most this many samples, so no more are handed over.
        val keptSamples = (maxDurationMs * active.sampleRate() / 1000L).toInt()
        fun handOver() {
            val until = minOf(active.sampleCount(), keptSamples)
            if (onSamples == null || until <= handedOver) return
            val pcm = active.pcmSince(handedOver).toByteArray()
            onSamples(WavUtils.pcmBytesToSamples(pcm).copyOf(until - handedOver), active.sampleRate())
            handedOver = until
        }
        try {
            while (active.elapsedMs() < maxDurationMs) {
                if (stopWhenRequested && stopRequested) break
                active.error()?.let { throw IllegalStateException(it) }
                if (active.sampleCount() - handedOver >= active.sampleRate() / 4) handOver()
                delay(20)
            }
        } catch (error: Throwable) {
            active.cancel()
            take = null
            throw error
        }
        val (bytes, _) = active.finishBytes()
        take = null

        val (pcm, sampleRate) = WavUtils.readPcm16Le(bytes)
        val maxSamples = (maxDurationMs * sampleRate / 1000L).toInt()
        val samples = WavUtils.pcmBytesToSamples(pcm).let { if (it.size > maxSamples) it.copyOf(maxSamples) else it }
        val peak = samples.maxOfOrNull { abs(it.toInt()) } ?: 0
        if (peak < 80) {
            throw IllegalStateException("صدایی ضبط نشد. میکروفون و دسترسی مرورگر به آن را بررسی کنید.")
        }
        // The rest from the finished take itself, so what was handed over is the whole take.
        if (onSamples != null && samples.size > handedOver) onSamples(samples.copyOfRange(handedOver, samples.size), sampleRate)
        WavUtils.writePcm16Wav(outputFile, WavUtils.normalizePeak(samples), sampleRate)
        return samples.size * 1000L / sampleRate
    }

    actual fun cancel() {
        take?.cancel()
        take = null
    }
}
