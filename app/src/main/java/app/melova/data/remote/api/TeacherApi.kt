package app.melova.data.remote.api

import app.melova.data.remote.dto.TeacherProfileDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

private const val TOKEN = "X-Teacher-Token"

/** Every call returns the teacher's whole profile with their lessons, already updated. */
interface TeacherApi {
    @GET("api/v1/teacher/me")
    suspend fun me(@Header(TOKEN) token: String): TeacherProfileDto

    @PUT("api/v1/teacher/me")
    suspend fun updateMe(@Header(TOKEN) token: String, @Body body: Map<String, String>): TeacherProfileDto

    @POST("api/v1/teacher/lessons")
    suspend fun createLesson(@Header(TOKEN) token: String, @Body body: Map<String, String>): TeacherProfileDto

    @PATCH("api/v1/teacher/lessons/{lessonId}")
    suspend fun updateLesson(
        @Header(TOKEN) token: String,
        @Path("lessonId") lessonId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): TeacherProfileDto

    @DELETE("api/v1/teacher/lessons/{lessonId}")
    suspend fun deleteLesson(@Header(TOKEN) token: String, @Path("lessonId") lessonId: String): TeacherProfileDto

    @Multipart
    @PUT("api/v1/teacher/lessons/{lessonId}/intro")
    suspend fun uploadIntro(
        @Header(TOKEN) token: String,
        @Path("lessonId") lessonId: String,
        @Part audio: MultipartBody.Part
    ): TeacherProfileDto

    @Multipart
    @POST("api/v1/teacher/lessons/{lessonId}/sections")
    suspend fun addSection(
        @Header(TOKEN) token: String,
        @Path("lessonId") lessonId: String,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part audio: MultipartBody.Part
    ): TeacherProfileDto

    @DELETE("api/v1/teacher/lessons/{lessonId}/sections/{sectionId}")
    suspend fun deleteSection(
        @Header(TOKEN) token: String,
        @Path("lessonId") lessonId: String,
        @Path("sectionId") sectionId: String
    ): TeacherProfileDto
}
