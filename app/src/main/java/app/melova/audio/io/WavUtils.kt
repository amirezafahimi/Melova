package app.melova.audio.io

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    fun writePcm16Wav(file: File, samples: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(pcm16MonoHeader(samples.size * 2, sampleRate))

            val chunk = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { chunk.putShort(it) }
            raf.write(chunk.array())
        }
    }

    /** The 44-byte header of a mono 16-bit PCM WAV holding [dataSize] bytes of samples. */
    fun pcm16MonoHeader(dataSize: Int, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val header = ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(dataSize)
        return header.array()
    }

    /**
     * Cuts the silence before the first and after the last sung note (keeping [marginMs] of
     * air) and normalizes the peak, so a reference recorded in the app starts on the music.
     * Returns the new duration in milliseconds.
     */
    fun trimSilenceAndNormalize(file: File, marginMs: Int = 250): Long {
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

    fun readPcm16Samples(file: File): Pair<ShortArray, Int> {
        val (bytes, sampleRate) = readPcm16Le(file)
        val samples = ShortArray(bytes.size / 2) { index ->
            val lo = bytes[index * 2].toInt() and 0xFF
            val hi = bytes[index * 2 + 1].toInt()
            ((hi shl 8) or lo).toShort()
        }
        return samples to sampleRate
    }

    fun readPcm16Le(file: File): Pair<ByteArray, Int> {
        val bytes = file.readBytes()
        require(bytes.size >= 44) { "Invalid WAV file" }

        val sampleRate = ByteBuffer.wrap(bytes, 24, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .getInt()

        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4)
            val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt()
            if (chunkId == "data") {
                val start = offset + 8
                val end = (start + chunkSize).coerceAtMost(bytes.size)
                return bytes.copyOfRange(start, end) to sampleRate
            }
            offset += 8 + chunkSize
        }

        throw IllegalArgumentException("WAV data chunk not found")
    }
}
