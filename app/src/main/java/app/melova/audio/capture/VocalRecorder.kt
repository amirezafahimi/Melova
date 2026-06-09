package app.melova.audio.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import app.melova.audio.io.WavUtils
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

class VocalRecorder(
    private val context: Context
) {
    @Volatile
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var stopRequested = false

    fun requestStop() {
        stopRequested = true
    }

    suspend fun recordFor(durationMs: Long, outputFile: File) = withContext(Dispatchers.IO) {
        recordInternal(
            outputFile = outputFile,
            maxDurationMs = durationMs,
            stopWhenRequested = false
        )
    }

    suspend fun recordFreeform(outputFile: File, maxDurationMs: Long): Long = withContext(Dispatchers.IO) {
        recordInternal(
            outputFile = outputFile,
            maxDurationMs = maxDurationMs,
            stopWhenRequested = true
        )
    }

    private fun recordInternal(
        outputFile: File,
        maxDurationMs: Long,
        stopWhenRequested: Boolean
    ): Long {
        stopRequested = false
        if (!hasRecordAudioPermission()) {
            throw SecurityException("دسترسی میکروفون داده نشده است")
        }

        val sampleRate = WavUtils.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuffer <= 0) {
            throw IllegalStateException("این دستگاه از ضبط صدا پشتیبانی نمی‌کند")
        }
        val bufferSize = maxOf(minBuffer, 4096)

        val audioSource = BuiltInMicrophoneSelector.chooseAudioSource(context)

        val recorder = try {
            AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: SecurityException) {
            throw SecurityException("دسترسی میکروفون رد شد", e)
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("میکروفون در دسترس نیست")
        }

        BuiltInMicrophoneSelector.preferBuiltInMicrophone(context, recorder)

        audioRecord = recorder
        val maxSamples = ((maxDurationMs * sampleRate) / 1000L).toInt()
        val samples = ShortArray(maxSamples)
        val readBuffer = ShortArray(bufferSize)
        var written = 0
        var idleReads = 0

        try {
            try {
                recorder.startRecording()
            } catch (e: SecurityException) {
                throw SecurityException("دسترسی میکروفون رد شد", e)
            }

            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IllegalStateException("میکروفون شروع به ضبط نکرد")
            }

            while (written < maxSamples) {
                if (stopWhenRequested && stopRequested) break
                val read = recorder.read(readBuffer, 0, readBuffer.size)
                when {
                    read > 0 -> {
                        idleReads = 0
                        val toCopy = minOf(read, maxSamples - written)
                        readBuffer.copyInto(samples, written, 0, toCopy)
                        written += toCopy
                    }
                    read == AudioRecord.ERROR_INVALID_OPERATION ||
                        read == AudioRecord.ERROR_BAD_VALUE -> {
                        throw IllegalStateException("خطا در خواندن میکروفون")
                    }
                    read == AudioRecord.ERROR_DEAD_OBJECT -> {
                        throw IllegalStateException("میکروفون قطع شد")
                    }
                    else -> {
                        idleReads++
                        if (idleReads > 100) {
                            throw IllegalStateException("میکروفون پاسخ نمی‌دهد")
                        }
                        Thread.sleep(10)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop()
            }
            recorder.release()
            audioRecord = null
        }

        val recordedSamples = samples.copyOf(written)
        val peak = recordedSamples.maxOfOrNull { abs(it.toInt()) } ?: 0
        if (peak < 80) {
            throw IllegalStateException(
                if (DeviceAudioHints.isEmulator) {
                    "صدایی از میکروفون امولیتور نیامد. Extended Controls → Microphone → Host audio input را فعال کنید."
                } else {
                    "صدایی ضبط نشد. میکروفون و دسترسی RECORD_AUDIO را بررسی کنید."
                }
            )
        }
        val normalized = WavUtils.normalizePeak(recordedSamples)
        WavUtils.writePcm16Wav(outputFile, normalized, sampleRate)
        return (written.toLong() * 1000L) / sampleRate
    }

    fun cancel() {
        audioRecord?.stop()
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
