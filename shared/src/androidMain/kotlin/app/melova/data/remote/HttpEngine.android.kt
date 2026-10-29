package app.melova.data.remote

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

internal actual fun httpEngine(): HttpClientEngineFactory<*> = Android
