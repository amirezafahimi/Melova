package app.melova.audio.io

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import app.melova.platform.appContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TIMEOUT_US = 10_000L

internal actual suspend fun compressedTake(wav: ByteArray): CompressedTake? = withContext(Dispatchers.Default) {
    // Any encoder trouble just sends the WAV, which the server reads as before.
    runCatching { encodeAac(wav) }.getOrNull()?.let { CompressedTake(it, "m4a", "audio/mp4") }
}

private fun encodeAac(wav: ByteArray): ByteArray? {
    val (pcm, sampleRate) = WavUtils.readPcm16Le(wav)
    if (pcm.isEmpty() || sampleRate <= 0) return null
    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
        setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        setInteger(MediaFormat.KEY_BIT_RATE, TAKE_BIT_RATE)
        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
    }
    val output = File.createTempFile("take", ".m4a", appContext.cacheDir)
    val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    val muxer = MediaMuxer(output.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    try {
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val info = MediaCodec.BufferInfo()
        var track = -1
        var offset = 0
        var inputDone = false
        while (true) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(TIMEOUT_US)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index) ?: return null
                    // Whole 16-bit samples only.
                    val size = minOf(buffer.remaining(), pcm.size - offset) and 1.inv()
                    buffer.put(pcm, offset, size)
                    val timeUs = offset / 2 * 1_000_000L / sampleRate
                    offset += size
                    inputDone = offset >= pcm.size - 1
                    val flags = if (inputDone) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    codec.queueInputBuffer(index, 0, size, timeUs, flags)
                }
            }
            val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                track = muxer.addTrack(codec.outputFormat)
                muxer.start()
            } else if (index >= 0) {
                val data = codec.getOutputBuffer(index)
                val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                if (!isConfig && info.size > 0 && track >= 0 && data != null) {
                    data.position(info.offset)
                    data.limit(info.offset + info.size)
                    muxer.writeSampleData(track, data, info)
                }
                codec.releaseOutputBuffer(index, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        if (track < 0) return null
        muxer.stop()
        return output.readBytes()
    } finally {
        runCatching { codec.stop() }
        codec.release()
        runCatching { muxer.release() }
        output.delete()
    }
}
