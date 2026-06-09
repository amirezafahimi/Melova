package app.melova.data.remote.api

import app.melova.data.remote.dto.HealthResponseDto
import app.melova.data.remote.dto.ScoringResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    @POST("api/v1/score")
    suspend fun score(@Body body: RequestBody): ScoringResponseDto
}
