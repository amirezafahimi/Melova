package app.melova.audio.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import app.melova.audio.io.WavUtils
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * A teacher's take recorded in the app, written to disk as it goes so a long take never sits
 * in memory. Unlike [VocalRecorder] it can pause, and it reports the input [level] for a meter.
 */
interface TeacherTakeRecorder {
    /** Recorded time so far, not counting pauses. */
    val elapsedMs: Long

    /** Loudness of the latest input, 0..1. */
    val level: Float

    /** Set when the microphone fails mid-take; the take is then unusable. */
    val error: String?

    fun start(output: File)
    fun pause()
    fun resume()

    /** Finishes the file and returns its duration; call off the main thread. */
    fun stop(): Long

    /** Stops without keeping anything. */
    fun cancel()

    companion object {
        /** Sections are scored against the student, so they stay lossless WAV. */
        fun lossless(context: Context): TeacherTakeRecorder = WavTakeRecorder(context)

        /** The lesson intro is talk that can run for many minutes, so it is AAC in an .m4a. */
        fun compressed(context: Context): TeacherTakeRecorder = M4aTakeRecorder(context)

        fun hasPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }
}

private class WavTakeRecorder(private val context: Context) : TeacherTakeRecorder {
    private val sampleRate = WavUtils.SAMPLE_RATE

    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var samplesWritten = 0L
    @Volatile override var level = 0f
        private set
    @Volatile override var error: String? = null
        private set

    private var thread: Thread? = null
    private var output: File? = null

    override val elapsedMs: Long
        get() = samplesWritten * 1000L / sampleRate

    @SuppressLint("MissingPermission")
    override fun start(output: File) {
        if (!TeacherTakeRecorder.hasPermission(context)) throw SecurityException("دسترسی میکروفون داده نشده است")
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        if (minBuffer <= 0) throw IllegalStateException("این دستگاه از ضبط صدا پشتیبانی نمی‌کند")
        val bufferSize = maxOf(minBuffer, 4096)
        val recorder = AudioRecord(BuiltInMicrophoneSelector.chooseAudioSource(context), sampleRate, channel, encoding, bufferSize)
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("میکروفون در دسترس نیست")
        }
        BuiltInMicrophoneSelector.preferBuiltInMicrophone(context, recorder)
        recorder.startRecording()
        if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            recorder.release()
            throw IllegalStateException("میکروفون شروع به ضبط نکرد؛ شاید برنامه‌ی دیگری از آن استفاده می‌کند")
        }

        this.output = output
        samplesWritten = 0L
        paused = false
        error = null
        running = true
        thread = Thread({ pump(recorder, output, bufferSize) }, "melova-teacher-take").apply { start() }
    }

    private fun pump(recorder: AudioRecord, output: File, bufferSize: Int) {
        val buffer = ShortArray(bufferSize)
        val bytes = ByteBuffer.allocate(bufferSize * 2).order(ByteOrder.LITTLE_ENDIAN)
        try {
            RandomAccessFile(output, "rw").use { raf ->
                raf.setLength(0)
                raf.write(ByteArray(WavUtils.WAV_HEADER_BYTES))
                while (running) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read < 0) throw IllegalStateException("میکروفون قطع شد")
                    if (read == 0) continue
                    var peak = 0
                    for (index in 0 until read) peak = maxOf(peak, abs(buffer[index].toInt()))
                    level = peak / Short.MAX_VALUE.toFloat()
                    if (paused) continue
                    bytes.clear()
                    for (index in 0 until read) bytes.putShort(buffer[index])
                    raf.write(bytes.array(), 0, read * 2)
                    samplesWritten += read
                }
                raf.seek(0)
                raf.write(WavUtils.pcm16MonoHeader((samplesWritten * 2).toInt(), sampleRate))
            }
        } catch (failure: Exception) {
            error = failure.message ?: "ضبط متوقف شد"
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
            level = 0f
        }
    }

    override fun pause() {
        paused = true
    }

    override fun resume() {
        paused = false
    }

    override fun stop(): Long {
        running = false
        thread?.join()
        thread = null
        error?.let { throw IllegalStateException(it) }
        return elapsedMs
    }

    override fun cancel() {
        running = false
        thread?.join()
        thread = null
        output?.delete()
    }
}

private class M4aTakeRecorder(private val context: Context) : TeacherTakeRecorder {
    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var startedAt = 0L
    private var pausedAt = 0L
    private var pausedTotal = 0L

    @Volatile override var error: String? = null
        private set

    override val elapsedMs: Long
        get() {
            if (startedAt == 0L) return 0L
            val now = if (pausedAt > 0L) pausedAt else SystemClock.elapsedRealtime()
            return now - startedAt - pausedTotal
        }

    override val level: Float
        get() = recorder?.let { runCatching { it.maxAmplitude / Short.MAX_VALUE.toFloat() }.getOrNull() } ?: 0f

    override fun start(output: File) {
        if (!TeacherTakeRecorder.hasPermission(context)) throw SecurityException("دسترسی میکروفون داده نشده است")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(WavUtils.SAMPLE_RATE)
            recorder.setAudioChannels(1)
            recorder.setAudioEncodingBitRate(128_000)
            recorder.setOutputFile(output.absolutePath)
            recorder.setOnErrorListener { _, _, _ -> error = "ضبط با خطا متوقف شد" }
            recorder.prepare()
            recorder.start()
        } catch (failure: Exception) {
            recorder.release()
            output.delete()
            throw IllegalStateException("میکروفون در دسترس نیست؛ شاید برنامه‌ی دیگری از آن استفاده می‌کند", failure)
        }
        this.recorder = recorder
        this.output = output
        error = null
        startedAt = SystemClock.elapsedRealtime()
        pausedAt = 0L
        pausedTotal = 0L
    }

    override fun pause() {
        if (pausedAt > 0L) return
        recorder?.pause()
        pausedAt = SystemClock.elapsedRealtime()
    }

    override fun resume() {
        if (pausedAt == 0L) return
        recorder?.resume()
        pausedTotal += SystemClock.elapsedRealtime() - pausedAt
        pausedAt = 0L
    }

    override fun stop(): Long {
        val duration = elapsedMs
        val recorder = recorder ?: return 0L
        this.recorder = null
        try {
            recorder.stop()
        } catch (failure: RuntimeException) {
            // MediaRecorder throws when it captured nothing usable, e.g. a take under a second.
            output?.delete()
            throw IllegalStateException("ضبط خیلی کوتاه بود", failure)
        } finally {
            recorder.release()
        }
        error?.let { throw IllegalStateException(it) }
        return duration
    }

    override fun cancel() {
        recorder?.let { recorder ->
            runCatching { recorder.stop() }
            recorder.release()
        }
        recorder = null
        output?.delete()
    }
}
