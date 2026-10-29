package app.melova.data.remote.api

import app.melova.data.remote.dto.HealthResponseDto
import app.melova.data.remote.dto.CatalogResponseDto
import app.melova.data.remote.dto.ScoringResponseDto
import app.melova.data.remote.dto.TeacherOrnamentsResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface MelovaApi {
    @GET("health")
    suspend fun health(): HealthResponseDto

    @Streaming
    @GET("api/v1/assets/teacher.wav")
    suspend fun downloadTeacherWav(): ResponseBody

    @Streaming
    @GET("api/v1/assets/teacher_syllables.json")
    suspend fun downloadTeacherSyllables(): ResponseBody

    /** Teachers and their published lessons; with a teacher code that teacher's drafts too. */
    @GET("api/v1/catalog")
    suspend fun catalog(@Header("X-Teacher-Token") teacherToken: String?): CatalogResponseDto

    @Streaming
    @GET("api/v1/lessons/{lessonId}/intro")
    suspend fun downloadLessonIntro(@Path("lessonId") lessonId: String): ResponseBody

    @Streaming
    @GET("api/v1/lessons/{lessonId}/sections/{sectionId}/audio")
    suspend fun downloadSectionAudio(
        @Path("lessonId") lessonId: String,
        @Path("sectionId") sectionId: String
    ): ResponseBody

    /** The teacher's tahrir and vibrato vowels in one section. */
    @GET("api/v1/lessons/{lessonId}/sections/{sectionId}/ornaments")
    suspend fun sectionOrnaments(
        @Path("lessonId") lessonId: String,
        @Path("sectionId") sectionId: String
    ): TeacherOrnamentsResponseDto

    @POST("api/v1/score")
    suspend fun score(@Body body: RequestBody): ScoringResponseDto
}
