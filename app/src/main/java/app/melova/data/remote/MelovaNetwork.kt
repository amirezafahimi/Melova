package app.melova.data.remote

import app.melova.BuildConfig
import app.melova.data.remote.api.MelovaApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MelovaNetwork {
    private val gson: Gson = GsonBuilder().create()

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
