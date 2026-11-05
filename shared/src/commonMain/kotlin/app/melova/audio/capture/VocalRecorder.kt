package app.melova.audio.capture

import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext

/** Records the student's voice as a peak-normalized mono 16-bit WAV. */
expect class VocalRecorder(context: PlatformContext) {
    fun requestStop()

    /** The loudest sample of the latest stretch the microphone gave, 0 to 1; 0 when not recording. */
    fun level(): Float

    /** Records exactly [durationMs]; returns the recorded length. */
    suspend fun recordFor(durationMs: Long, outputFile: LocalFile): Long

    /** Records until [requestStop] or [maxDurationMs]; returns the recorded length. */
    suspend fun recordFreeform(outputFile: LocalFile, maxDurationMs: Long): Long

    fun cancel()
}
