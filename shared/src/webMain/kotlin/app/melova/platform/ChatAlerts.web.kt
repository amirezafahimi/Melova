package app.melova.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.melova.audio.web.MelovaAudioJs
import app.melova.data.remote.MelovaNetwork
import kotlin.js.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

actual object ChatAlerts {
    actual suspend fun state(): NotificationState = when (MelovaAudioJs.pushState().text()) {
        "on" -> NotificationState.ON
        "off" -> NotificationState.OFF
        "blocked" -> NotificationState.BLOCKED
        "needs-install" -> NotificationState.NEEDS_INSTALL
        else -> NotificationState.UNSUPPORTED
    }

    actual suspend fun turnOn(serverKey: suspend () -> String): PushAddress? =
        MelovaAudioJs.pushSubscribe(serverKey()).address()

    actual suspend fun turnOff(): PushAddress? = MelovaAudioJs.pushUnsubscribe().address()

    actual suspend fun address(): PushAddress? = MelovaAudioJs.pushAddress().address()

    private suspend fun Promise<JsAny?>.text(): String? = await<JsAny?>()?.unsafeCast<JsString>()?.toString()

    private suspend fun Promise<JsAny?>.address(): PushAddress? =
        text()?.let { MelovaNetwork.json.decodeFromString<PushAddress>(it) }
}

private val permissionScope: CoroutineScope = MainScope()

/** iOS answers only inside the tap that asks, so this runs straight from it. */
@Composable
actual fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit =
    remember(onResult) {
        {
            val answer = MelovaAudioJs.askNotifications()
            permissionScope.launch {
                onResult(runCatching { answer.await<JsAny?>()?.unsafeCast<JsBoolean>()?.toBoolean() == true }.getOrDefault(false))
            }
        }
    }
