package app.melova.audio.io

import app.melova.platform.LocalFile

object AudioFileDecoder {
    fun isPcmWav(file: LocalFile): Boolean {
        return runCatching {
            val bytes = file.readBytes()
            require(bytes.size >= 44) { "too small" }
            val riff = bytes.decodeToString(0, 4)
            val wave = bytes.decodeToString(8, 12)
            val fmt = bytes.decodeToString(12, 16)
            val audioFormat = WavUtils.readLeShort(bytes, 20)
            riff == "RIFF" && wave == "WAVE" && fmt == "fmt " && audioFormat == 1.toShort()
        }.getOrDefault(false)
    }
}
