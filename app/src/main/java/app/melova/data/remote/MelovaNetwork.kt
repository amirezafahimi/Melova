package app.melova.data.remote

import app.melova.BuildConfig
import app.melova.data.remote.api.MelovaApi
import app.melova.data.remote.api.StudentApi
import app.melova.data.remote.api.TeacherApi
import app.melova.data.remote.api.TeacherDeskApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MelovaNetwork {
    private val gson: Gson = GsonBuilder().create()

    /**
     * This install's student token, sent as X-Student-Token on every request so the server
     * knows which lessons are open to the student. Set from [app.melova.data.local.StudentSession].
     */
    @Volatile
    var studentToken: String? = null

    private val baseClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val token = studentToken
                chain.proceed(
                    if (token == null || request.header(STUDENT_HEADER) != null) request
                    else request.newBuilder().header(STUDENT_HEADER, token).build()
                )
            }
            .addInterceptor(logging)
            .build()
    }

    /** Asset downloads (teacher wav/json). */
    val api: MelovaApi by lazy { createApi(client = baseClient) }

    /** Scoring uploads can take longer — separate client with extended timeouts. */
    val scoringApi: MelovaApi by lazy {
        createApi(
            client = baseClient.newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build()
        )
    }

    /** Teacher panel; uploads of lesson audio can be large. */
    val teacherApi: TeacherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.SCORING_API_BASE_URL)
            .client(
                baseClient.newBuilder()
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TeacherApi::class.java)
    }

    val studentApi: StudentApi by lazy { retrofit(teacherUploadClient).create(StudentApi::class.java) }

    val teacherDeskApi: TeacherDeskApi by lazy { retrofit(teacherUploadClient).create(TeacherDeskApi::class.java) }

    /** Takes and voice replies go up and down through these; give them time. */
    private val teacherUploadClient: OkHttpClient by lazy {
        baseClient.newBuilder()
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.SCORING_API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private const val STUDENT_HEADER = "X-Student-Token"

    fun createApi(
        baseUrl: String = BuildConfig.SCORING_API_BASE_URL,
        client: OkHttpClient = baseClient
    ): MelovaApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MelovaApi::class.java)
    }
}
