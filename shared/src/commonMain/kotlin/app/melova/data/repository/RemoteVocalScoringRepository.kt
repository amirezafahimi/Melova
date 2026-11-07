package app.melova.data.repository

import app.melova.audio.io.FlacEncoder
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
        streamId: String?
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
            streamId?.let { put("stream_id", it) }
        }
        val body = multipartBody(
            fields,
            listOf(takePart("student", studentFile.readBytes()))
        )

        return try {
            val response = remoteDataSource.scorePerformance(body)
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
