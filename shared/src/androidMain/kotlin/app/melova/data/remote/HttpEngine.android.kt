package app.melova.data.remote

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun httpEngine(): HttpClientEngineFactory<*> = Android

internal actual fun socketEngine(): HttpClientEngineFactory<*> = OkHttp
