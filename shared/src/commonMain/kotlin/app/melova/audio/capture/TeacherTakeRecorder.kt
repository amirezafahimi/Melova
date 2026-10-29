package app.melova.audio.capture

import app.melova.platform.LocalFile
import app.melova.platform.PlatformContext

/**
 * A teacher's take recorded in the app. Unlike [VocalRecorder] it can pause, and it reports
 * the input [level] for a meter.
 */
interface TeacherTakeRecorder {
    /** Recorded time so far, not counting pauses. */
    val elapsedMs: Long

    /** Loudness of the latest input, 0..1. */
    val level: Float

    /** Set when the microphone fails mid-take; the take is then unusable. */
    val error: String?

    /** The file extension this recorder writes, without the dot. */
    val extension: String

    suspend fun start(output: LocalFile)
    fun pause()
    fun resume()

    /** Finishes the file and returns its duration. */
    suspend fun stop(): Long

    /** Stops without keeping anything. */
    fun cancel()

    companion object {
        /** Sections are scored against the student, so they stay lossless WAV. */
        fun lossless(context: PlatformContext): TeacherTakeRecorder = losslessTakeRecorder(context)

        /** The lesson intro is talk that can run for many minutes, so it is compressed. */
        fun compressed(context: PlatformContext): TeacherTakeRecorder = compressedTakeRecorder(context)
    }
}

internal expect fun losslessTakeRecorder(context: PlatformContext): TeacherTakeRecorder

internal expect fun compressedTakeRecorder(context: PlatformContext): TeacherTakeRecorder
