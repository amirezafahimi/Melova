package app.melova.data.repository

import app.melova.BuildConfig
import app.melova.audio.playback.ExpectedSegment
import app.melova.data.model.VocalScoreResult
import app.melova.data.remote.datasource.MelovaRemoteDataSource
import app.melova.data.remote.datasource.MelovaRemoteDataSourceImpl
import app.melova.data.remote.mapper.toDomain
import app.melova.data.remote.mapper.toRequestJson
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException

class RemoteVocalScoringRepository(
    private val remoteDataSource: MelovaRemoteDataSource = MelovaRemoteDataSourceImpl()
) : VocalScoringRepository {

    override suspend fun score(
        referenceFile: File,
        studentFile: File,
        expectedSegments: List<ExpectedSegment>?,
        lessonId: String?,
        sectionId: String?
    ): VocalScoreResult {
        require(studentFile.exists() && studentFile.length() > 44L) {
            "فایل ضبط هنرجو یافت نشد"
        }

        pingServer()

        val wavMediaType = "audio/wav".toMediaType()
        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "student",
                filename = studentFile.name,
                body = studentFile.asRequestBody(wavMediaType)
            )

        if (lessonId != null && sectionId != null) {
            multipartBuilder.addFormDataPart("lesson_id", lessonId)
            multipartBuilder.addFormDataPart("section_id", sectionId)
        }

        expectedSegments.toRequestJson()?.let { json ->
            multipartBuilder.addFormDataPart("expected_segments_json", json)
        }

        return try {
            remoteDataSource.scorePerformance(multipartBuilder.build()).toDomain()
        } catch (error: HttpException) {
            val detail = error.response()?.errorBody()?.string()?.trim().orEmpty()
            throw IOException(
                if (detail.isNotEmpty()) "HTTP ${error.code()}: $detail"
                else "HTTP ${error.code()} ${error.message()}",
                error
            )
        } catch (error: ConnectException) {
            throw IOException(
                "اتصال به سرور برقرار نشد (${BuildConfig.SCORING_API_BASE_URL}). " +
                    "backend/run_server.sh را اجرا کنید و IP در local.properties را بررسی کنید.",
                error
            )
        } catch (error: SocketTimeoutException) {
            throw IOException(
                "زمان انتظار تمام شد — سرور ${BuildConfig.SCORING_API_BASE_URL} پاسخ نداد.",
                error
            )
        }
    }

    private suspend fun pingServer() {
        try {
            remoteDataSource.getHealth()
        } catch (error: ConnectException) {
            throw IOException(
                "سرور در دسترس نیست (${BuildConfig.SCORING_API_BASE_URL}). " +
                    "اول ./backend/run_server.sh را اجرا کنید.",
                error
            )
        }
    }
}
