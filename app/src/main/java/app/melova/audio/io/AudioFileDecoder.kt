package app.melova.audio.io

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioFileDecoder {
    fun isPcmWav(file: File): Boolean {
        return runCatching {
            val bytes = file.readBytes()
            require(bytes.size >= 44) { "too small" }
            val riff = String(bytes, 0, 4)
            val wave = String(bytes, 8, 4)
            val fmt = String(bytes, 12, 4)
            val audioFormat = ByteBuffer.wrap(bytes, 20, 2).order(ByteOrder.LITTLE_ENDIAN).short
            riff == "RIFF" && wave == "WAVE" && fmt == "fmt " && audioFormat == 1.toShort()
        }.getOrDefault(false)
    }
}
