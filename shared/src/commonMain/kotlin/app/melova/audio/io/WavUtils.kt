package app.melova.audio.io

import app.melova.platform.LocalFile
import kotlin.math.abs

object WavUtils {
    const val SAMPLE_RATE = 44100
    const val WAV_HEADER_BYTES = 44

    fun normalizePeak(samples: ShortArray, targetPeakRatio: Float = 0.92f): ShortArray {
        if (samples.isEmpty()) return samples

        var peak = 0
        for (sample in samples) {
            peak = maxOf(peak, abs(sample.toInt()))
        }
        if (peak <= 0) return samples

        val targetPeak = (Short.MAX_VALUE * targetPeakRatio).toInt()
        val gain = targetPeak.toDouble() / peak
        if (gain in 0.995..1.005) return samples

        val cappedGain = gain.coerceIn(0.5, 12.0)
        return ShortArray(samples.size) { index ->
            (samples[index] * cappedGain).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    fun writePcm16Wav(file: LocalFile, samples: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        file.writeBytes(pcm16Wav(samples, sampleRate))
    }

    /** A whole mono 16-bit PCM WAV holding [samples]. */
    fun pcm16Wav(samples: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val bytes = pcm16MonoHeader(samples.size * 2, sampleRate).copyOf(WAV_HEADER_BYTES + samples.size * 2)
        samples.forEachIndexed { index, sample -> writeLeShort(bytes, WAV_HEADER_BYTES + index * 2, sample) }
        return bytes
    }

    /** The 44-byte header of a mono 16-bit PCM WAV holding [dataSize] bytes of samples. */
    fun pcm16MonoHeader(dataSize: Int, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val header = ByteArray(WAV_HEADER_BYTES)
        "RIFF".encodeToByteArray().copyInto(header, 0)
        writeLeInt(header, 4, 36 + dataSize)
        "WAVE".encodeToByteArray().copyInto(header, 8)
        "fmt ".encodeToByteArray().copyInto(header, 12)
        writeLeInt(header, 16, 16)
        writeLeShort(header, 20, 1)
        writeLeShort(header, 22, 1)
        writeLeInt(header, 24, sampleRate)
        writeLeInt(header, 28, sampleRate * 2)
        writeLeShort(header, 32, 2)
        writeLeShort(header, 34, 16)
        "data".encodeToByteArray().copyInto(header, 36)
        writeLeInt(header, 40, dataSize)
        return header
    }

    /**
     * Cuts the silence before the first and after the last sung note (keeping [marginMs] of
     * air) and normalizes the peak, so a reference recorded in the app starts on the music.
     * Returns the new duration in milliseconds.
     */
    fun trimSilenceAndNormalize(file: LocalFile, marginMs: Int = 250): Long {
        val (samples, sampleRate) = readPcm16Samples(file)
        if (samples.isEmpty()) return 0L
        var peak = 0
        for (sample in samples) peak = maxOf(peak, abs(sample.toInt()))
        val threshold = maxOf(300, peak / 25)
        val first = samples.indexOfFirst { abs(it.toInt()) >= threshold }
        val last = samples.indexOfLast { abs(it.toInt()) >= threshold }
        val trimmed = if (first < 0) {
            samples
        } else {
            val margin = sampleRate * marginMs / 1000
            samples.copyOfRange((first - margin).coerceAtLeast(0), (last + margin + 1).coerceAtMost(samples.size))
        }
        writePcm16Wav(file, normalizePeak(trimmed), sampleRate)
        return trimmed.size * 1000L / sampleRate
    }

    fun readPcm16Samples(file: LocalFile): Pair<ShortArray, Int> {
        val (bytes, sampleRate) = readPcm16Le(file)
        return pcmBytesToSamples(bytes) to sampleRate
    }

    fun pcmBytesToSamples(bytes: ByteArray): ShortArray = ShortArray(bytes.size / 2) { index ->
        val lo = bytes[index * 2].toInt() and 0xFF
        val hi = bytes[index * 2 + 1].toInt()
        ((hi shl 8) or lo).toShort()
    }

    fun readPcm16Le(file: LocalFile): Pair<ByteArray, Int> = readPcm16Le(file.readBytes())

    fun readPcm16Le(bytes: ByteArray): Pair<ByteArray, Int> {
        require(bytes.size >= 44) { "Invalid WAV file" }

        val sampleRate = readLeInt(bytes, 24)

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkId = bytes.decodeToString(offset, offset + 4)
            val chunkSize = readLeInt(bytes, offset + 4)
            if (chunkId == "data") {
                val start = offset + 8
                val end = (start + chunkSize).coerceAtMost(bytes.size)
                return bytes.copyOfRange(start, end) to sampleRate
            }
            offset += 8 + chunkSize
        }

        throw IllegalArgumentException("WAV data chunk not found")
    }

    internal fun readLeInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    internal fun readLeShort(bytes: ByteArray, offset: Int): Short =
        ((bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)).toShort()

    private fun writeLeInt(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value shr 8).toByte()
        bytes[offset + 2] = (value shr 16).toByte()
        bytes[offset + 3] = (value shr 24).toByte()
    }

    private fun writeLeShort(bytes: ByteArray, offset: Int, value: Short) {
        bytes[offset] = value.toInt().toByte()
        bytes[offset + 1] = (value.toInt() shr 8).toByte()
    }
}
