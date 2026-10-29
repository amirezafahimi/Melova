package app.melova.data.remote

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

internal actual fun httpEngine(): HttpClientEngineFactory<*> = Js
