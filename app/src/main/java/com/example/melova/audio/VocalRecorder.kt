package com.example.melova.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class VocalRecorder(
    private val context: Context
) {
    @Volatile
    private var audioRecord: AudioRecord? = null

    suspend fun recordFor(durationMs: Long, outputFile: File) = withContext(Dispatchers.IO) {
        if (!hasRecordAudioPermission()) {
            throw SecurityException("دسترسی میکروفون داده نشده است")
        }

        val sampleRate = WavUtils.SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
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
        val totalSamples = ((durationMs * sampleRate) / 1000L).toInt()
        val samples = ShortArray(totalSamples)
        val readBuffer = ShortArray(bufferSize)
        var written = 0

        try {
            try {
                recorder.startRecording()
            } catch (e: SecurityException) {
                throw SecurityException("دسترسی میکروفون رد شد", e)
            }

            while (written < totalSamples) {
                val read = recorder.read(readBuffer, 0, readBuffer.size)
                if (read <= 0) continue
                val toCopy = minOf(read, totalSamples - written)
                readBuffer.copyInto(samples, written, 0, toCopy)
                written += toCopy
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

        WavUtils.writePcm16Wav(outputFile, samples, sampleRate)
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
