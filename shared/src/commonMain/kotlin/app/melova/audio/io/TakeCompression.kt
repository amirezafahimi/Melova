package app.melova.audio.io

import app.melova.data.remote.FilePart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Same as TAKE_MARKER_MS and TAKE_MARKER_CLICK_MS in the backend's audio_upload.py.
private const val MARKER_MS = 250
private const val MARKER_CLICK_MS = 100

/**
 * A take as one part of an upload, as FLAC: about a third of the WAV and the same samples.
 * Lossy AAC was smaller, but changes no one could hear moved a take's score from 34 to 48.
 * A take that isn't a readable WAV goes as it is.
 */
internal suspend fun takePart(name: String, wav: ByteArray): FilePart = withContext(Dispatchers.Default) {
    runCatching {
        val (pcm, sampleRate) = WavUtils.readPcm16Le(withTakeMarker(wav))
        FlacEncoder.encode(WavUtils.pcmBytesToSamples(pcm), sampleRate)
    }.getOrNull()?.let { FilePart(name, "take.flac", "audio/flac", it) }
        ?: FilePart(name, "take.wav", "audio/wav", wav)
}

/**
 * The take after a short marker: silence with one click. The server finds the click and cuts
 * the marker off any upload that isn't a WAV; it was made for lossy encoders, which put a
 * lead-in of their own before the take.
 */
internal fun withTakeMarker(wav: ByteArray): ByteArray {
    val (pcm, sampleRate) = WavUtils.readPcm16Le(wav)
    val marker = ShortArray(sampleRate * MARKER_MS / 1000)
    marker[sampleRate * MARKER_CLICK_MS / 1000] = (Short.MAX_VALUE * 0.95f).toInt().toShort()
    return WavUtils.pcm16Wav(marker + WavUtils.pcmBytesToSamples(pcm), sampleRate)
}
