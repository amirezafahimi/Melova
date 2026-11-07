package app.melova.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile
import kotlin.random.Random

/**
 * A take sent to the server a second at a time while it is sung, so most of its phoneme analysis
 * is done before the student sends it. The student sees nothing of it: a result still comes only
 * from scoring with [id]. Pieces go one after another; once one fails no more go, and the server
 * analyses the whole take as before. When all of them arrived the take isn't sent again: on a slow
 * line that doubled the wait.
 */
class TakeStream(
    val id: String,
    private val scope: CoroutineScope,
    private val send: suspend (offset: Int, samples: ShortArray, sampleRate: Int, ended: Boolean) -> Boolean,
    private val drop: suspend () -> Unit
) {
    private sealed interface Event
    private class Samples(val samples: ShortArray, val sampleRate: Int) : Event
    private data object End : Event

    private val events = Channel<Event>(Channel.UNLIMITED)

    /** The server has the take with this id; [whole] when every piece of it arrived. */
    class Streamed(val id: String, val whole: Boolean)

    @Volatile
    private var failed = false

    // Moves with every piece sent, and once more when the sending stops.
    private val progress = MutableStateFlow(0)

    private val sending = scope.launch {
        var offset = 0
        var sampleRate = 0
        var pending = ShortArray(0)
        var count = 0

        suspend fun sendPiece(size: Int, ended: Boolean) {
            val piece = pending.copyOf(size)
            pending.copyInto(pending, 0, size, count)
            count -= size
            if (failed) return
            failed = try {
                !send(offset, piece, sampleRate, ended)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                true
            }
            offset += size
            progress.value++
        }

        for (event in events) {
            when (event) {
                is Samples -> {
                    sampleRate = event.sampleRate
                    if (count + event.samples.size > pending.size) {
                        pending = pending.copyOf(maxOf(pending.size * 2, count + event.samples.size, sampleRate * 2))
                    }
                    event.samples.copyInto(pending, count)
                    count += event.samples.size
                    while (count >= sampleRate) sendPiece(sampleRate, ended = false)
                }
                End -> if (sampleRate > 0) sendPiece(count, ended = true)
            }
        }
    }

    /** From any thread, the recorder's included. */
    fun add(samples: ShortArray, sampleRate: Int) {
        if (samples.isNotEmpty() && sampleRate > 0) events.trySend(Samples(samples, sampleRate))
    }

    /** The take is over: what is left goes as its last piece. */
    fun end() {
        events.trySend(End)
        events.close()
    }

    init {
        sending.invokeOnCompletion { progress.value++ }
    }

    /**
     * Waits for the pieces still going while they keep going; null when a piece failed. Pieces left
     * when they stop moving are dropped, so they don't hold up the take sent in their place.
     */
    suspend fun streamed(): Streamed? {
        while (sending.isActive) {
            val before = progress.value
            if (withTimeoutOrNull(STALL_MS) { progress.first { it != before } } == null) {
                sending.cancel()
                break
            }
        }
        if (failed) return null
        return Streamed(id, whole = sending.isCompleted && !sending.isCancelled)
    }

    /** The take was thrown away; the server forgets its pieces. */
    fun discard() {
        events.close()
        sending.cancel()
        scope.launch {
            try {
                drop()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                // The server forgets it on its own after a few minutes.
            }
        }
    }

    companion object {
        // A second of FLAC takes a few seconds on the slowest lines seen; longer than this, it's stuck.
        private const val STALL_MS = 15_000L

        fun newId(): String = Random.nextBytes(16).joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }
}
