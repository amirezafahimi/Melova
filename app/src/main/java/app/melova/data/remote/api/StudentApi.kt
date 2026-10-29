package app.melova.data.remote.api

import app.melova.data.remote.dto.CreatedStudentDto
import app.melova.data.remote.dto.ReviewsDto
import app.melova.data.remote.dto.StudentMeDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

/** The student's own account; the token header is added by [app.melova.data.remote.MelovaNetwork]. */
interface StudentApi {
    @POST("api/v1/students")
    suspend fun create(@Body body: Map<String, String>): CreatedStudentDto

    @GET("api/v1/me")
    suspend fun me(): StudentMeDto

    @PUT("api/v1/me")
    suspend fun updateMe(@Body body: Map<String, String>): StudentMeDto

    /** Start a free lesson: it comes with its included teacher reviews. */
    @POST("api/v1/me/lessons/{lessonId}/claim")
    suspend fun claimFreeLesson(@Path("lessonId") lessonId: String): StudentMeDto

    @Multipart
    @POST("api/v1/me/reviews")
    suspend fun sendForReview(
        @Part("lesson_id") lessonId: RequestBody,
        @Part("section_id") sectionId: RequestBody,
        @Part("note") note: RequestBody,
        @Part("referral_id") referralId: RequestBody?,
        @Part("score_json") scoreJson: RequestBody?,
        @Part audio: MultipartBody.Part
    ): StudentMeDto

    @GET("api/v1/me/reviews")
    suspend fun reviews(): ReviewsDto

    @Streaming
    @GET("api/v1/me/reviews/{reviewId}/reply-audio")
    suspend fun replyAudio(@Path("reviewId") reviewId: String): ResponseBody
}
