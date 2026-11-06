package app.melova.audio.io

import app.melova.data.remote.FilePart

/** A take encoded as AAC: m4a on Android, ADTS frames in the browser. */
internal class CompressedTake(val bytes: ByteArray, val extension: String, val mimeType: String)

/**
 * A mono 16-bit WAV take as AAC, or null when this device can't encode it.
 * The server decodes it back to the same WAV; a WAV upload is about seven times bigger.
 */
internal expect suspend fun compressedTake(wav: ByteArray): CompressedTake?

/**
 * AAC at this rate moved eight saved takes' scores by at most two points, either way: about as
 * much as the same file scored on two machines.
 */
internal const val TAKE_BIT_RATE = 96_000

// Same as TAKE_MARKER_MS and TAKE_MARKER_CLICK_MS in the backend's audio_upload.py.
private const val MARKER_MS = 250
private const val MARKER_CLICK_MS = 100

/** A take as one part of an upload: compressed when the device can, otherwise the WAV itself. */
internal suspend fun takePart(name: String, wav: ByteArray): FilePart =
    compressedTake(withTakeMarker(wav))?.let { FilePart(name, "take.${it.extension}", it.mimeType, it.bytes) }
        ?: FilePart(name, "take.wav", "audio/wav", wav)

/**
 * The take after a short marker: silence with one click. An encoder adds a lead-in of its own
 * that the file doesn't record, and the take arriving that late moved scores; the server finds
 * the click and cuts to where the take starts.
 */
internal fun withTakeMarker(wav: ByteArray): ByteArray {
    val (pcm, sampleRate) = WavUtils.readPcm16Le(wav)
    val marker = ShortArray(sampleRate * MARKER_MS / 1000)
    marker[sampleRate * MARKER_CLICK_MS / 1000] = (Short.MAX_VALUE * 0.95f).toInt().toShort()
    return WavUtils.pcm16Wav(marker + WavUtils.pcmBytesToSamples(pcm), sampleRate)
}
