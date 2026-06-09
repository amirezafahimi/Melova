package app.melova.audio.io

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object WavUtils {
    const val SAMPLE_RATE = 44100

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
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize

        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalSize)
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
            raf.write(header.array())

            val chunk = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { chunk.putShort(it) }
            raf.write(chunk.array())
        }
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
