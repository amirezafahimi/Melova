package app.melova.data.remote

import io.ktor.client.engine.HttpClientEngineFactory

/** OkHttp on Android, fetch() in the browser. */
internal expect fun httpEngine(): HttpClientEngineFactory<*>

/** Ktor's Android engine has no WebSockets. */
internal expect fun socketEngine(): HttpClientEngineFactory<*>
