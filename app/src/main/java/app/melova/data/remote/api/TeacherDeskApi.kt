package app.melova.data.remote.api

import app.melova.data.remote.dto.ReviewDto
import app.melova.data.remote.dto.ReviewsDto
import app.melova.data.remote.dto.StudentCardDto
import app.melova.data.remote.dto.StudentsDto
import app.melova.data.remote.dto.TeacherReportDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

private const val TOKEN = "X-Teacher-Token"

/** The teacher's desk: takes students sent, the students themselves, and the hours report. */
interface TeacherDeskApi {
    /** [status] open, answered, refunded, or empty for all; oldest first. */
    @GET("api/v1/teacher/reviews")
    suspend fun reviews(@Header(TOKEN) token: String, @Query("status") status: String): ReviewsDto

    /** Opening a review the first time starts its clock on the server. */
    @GET("api/v1/teacher/reviews/{reviewId}")
    suspend fun review(@Header(TOKEN) token: String, @Path("reviewId") reviewId: String): ReviewDto

    @Streaming
    @GET("api/v1/teacher/reviews/{reviewId}/audio")
    suspend fun reviewAudio(@Header(TOKEN) token: String, @Path("reviewId") reviewId: String): ResponseBody

    @Multipart
    @POST("api/v1/teacher/reviews/{reviewId}/reply")
    suspend fun reply(
        @Header(TOKEN) token: String,
        @Path("reviewId") reviewId: String,
        @Part("text") text: RequestBody,
        @Part("minutes") minutes: RequestBody?,
        @Part audio: MultipartBody.Part?
    ): ReviewDto

    @POST("api/v1/teacher/reviews/{reviewId}/decline")
    suspend fun decline(
        @Header(TOKEN) token: String,
        @Path("reviewId") reviewId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): ReviewDto

    @GET("api/v1/teacher/students")
    suspend fun students(@Header(TOKEN) token: String): StudentsDto

    /** A payment the teacher took directly (card to card): a lesson, reviews, or both. */
    @POST("api/v1/teacher/students/{studentId}/payments")
    suspend fun recordPayment(
        @Header(TOKEN) token: String,
        @Path("studentId") studentId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): StudentCardDto

    @POST("api/v1/teacher/time")
    suspend fun logTime(@Header(TOKEN) token: String, @Body body: Map<String, @JvmSuppressWildcards Any>)

    @GET("api/v1/teacher/report")
    suspend fun report(
        @Header(TOKEN) token: String,
        @Query("days") days: Int,
        @Query("hourly_rate_toman") hourlyRateToman: Long?
    ): TeacherReportDto
}
