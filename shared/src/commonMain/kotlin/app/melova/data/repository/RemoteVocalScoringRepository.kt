package app.melova.data.repository

import app.melova.audio.io.FlacEncoder
import app.melova.audio.io.WavUtils
import app.melova.audio.io.atSpeechRate
import app.melova.audio.io.crc32
import app.melova.audio.io.takePart
import app.melova.audio.playback.ExpectedSegment
import app.melova.data.model.VocalScoreResult
import app.melova.data.remote.FilePart
import app.melova.data.remote.MelovaNetwork
import app.melova.data.remote.datasource.MelovaRemoteDataSource
import app.melova.data.remote.datasource.MelovaRemoteDataSourceImpl
import app.melova.data.remote.dto.ScoringResponseDto
import app.melova.data.remote.mapper.toDomain
import app.melova.data.remote.mapper.toRequestJson
import app.melova.data.remote.multipartBody
import app.melova.data.remote.serverBody
import app.melova.platform.LocalFile
import app.melova.platform.persianDigits
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

class RemoteVocalScoringRepository(
    private val remoteDataSource: MelovaRemoteDataSource = MelovaRemoteDataSourceImpl()
) : VocalScoringRepository {

    override suspend fun score(
        referenceFile: LocalFile,
        studentFile: LocalFile,
        expectedSegments: List<ExpectedSegment>?,
        lessonId: String?,
        sectionId: String?,
        streamed: TakeStream.Streamed?
    ): VocalScoreResult {
        require(studentFile.exists() && studentFile.length() > 44L) {
            "فایل ضبط هنرجو یافت نشد"
        }

        pingServer()

        val fields = buildMap {
            if (lessonId != null && sectionId != null) {
                put("lesson_id", lessonId)
                put("section_id", sectionId)
            }
            expectedSegments.toRequestJson()?.let { json -> put("expected_segments_json", json) }
            streamed?.let { put("stream_id", it.id) }
        }
        val take = speechRateTake(studentFile.readBytes())

        return try {
            val response = streamed?.takeIf { it.whole }?.let { scoreStreamedTake(fields, take, it.peak) }
                ?: remoteDataSource.scorePerformance(multipartBody(fields, listOf(takePart("student", take))))
            response.toDomain().copy(
                sourceJson = MelovaNetwork.json.encodeToString(ScoringResponseDto.serializer(), response)
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: ResponseException) {
            val code = error.response.status.value
            val detail = serverBody(error)
            throw IOException(
                persianDigits(detail.ifEmpty { "خطایی پیش آمد ($code)؛ دوباره تلاش کنید." }),
                error
            )
        } catch (error: HttpRequestTimeoutException) {
            throw IOException(
                "زمان انتظار تمام شد؛ دوباره تلاش کنید.",
                error
            )
        } catch (error: Throwable) {
            throw IOException(
                "اتصال برقرار نشد؛ اینترنت را بررسی کنید.",
                error
            )
        }
    }

    /** The take at the rate the models hear it at: a third of the upload, the score kept (see SpeechRate.kt). */
    private suspend fun speechRateTake(wav: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        runCatching {
            val (pcm, sampleRate) = WavUtils.readPcm16Le(wav)
            val (samples, rate) = atSpeechRate(WavUtils.pcmBytesToSamples(pcm), sampleRate)
            WavUtils.pcm16Wav(samples, rate)
        }.getOrDefault(wav)
    }

    /** Scores the take from its pieces on the server; null when the server asks for the take itself. */
    private suspend fun scoreStreamedTake(fields: Map<String, String?>, take: ByteArray, peak: Int): ScoringResponseDto? {
        val pcm = WavUtils.readPcm16Le(take).first
        val checked = fields + mapOf(
            "take_crc" to crc32(pcm).toString(),
            "take_samples" to (pcm.size / 2).toString(),
            "take_peak" to peak.toString()
        )
        return try {
            remoteDataSource.scorePerformance(multipartBody(checked))
        } catch (error: ResponseException) {
            // 422: a server from before this, which wants the take every time.
            if (error.response.status != HttpStatusCode.Conflict &&
                error.response.status != HttpStatusCode.UnprocessableEntity
            ) throw error
            null
        }
    }

    override fun streamTake(scope: CoroutineScope, lessonId: String?): TakeStream {
        val id = TakeStream.newId()
        return TakeStream(
            id,
            scope,
            send = { offset, samples, sampleRate, ended ->
                val piece = if (samples.isEmpty()) {
                    emptyList()
                } else {
                    val flac = withContext(Dispatchers.Default) { FlacEncoder.encode(samples, sampleRate) }
                    listOf(FilePart("piece", "piece.flac", "audio/flac", flac))
                }
                val fields = mapOf("offset" to offset.toString(), "ended" to ended.toString(), "lesson_id" to lessonId)
                remoteDataSource.streamTakePiece(id, multipartBody(fields, piece)).accepted
            },
            drop = { remoteDataSource.dropTakeStream(id) }
        )
    }

    private suspend fun pingServer() {
        try {
            remoteDataSource.getHealth()
        } catch (error: CancellationException) {
            throw error
        } catch (error: ResponseException) {
            throw error
        } catch (error: Throwable) {
            throw IOException(
                "اتصال برقرار نشد؛ اینترنت را بررسی کنید.",
                error
            )
        }
    }
}
