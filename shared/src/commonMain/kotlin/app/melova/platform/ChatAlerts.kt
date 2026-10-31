package app.melova.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

enum class NotificationState { UNSUPPORTED, NEEDS_INSTALL, OFF, ON, BLOCKED }

/** Where a browser's push service takes messages for this device. */
@Serializable
class PushAddress(val endpoint: String, val p256dh: String, val auth: String)

/**
 * Telling the person about new messages while the app is away. A browser gets them pushed
 * through a [PushAddress]; the Android app has none and looks for them itself every so often.
 */
expect object ChatAlerts {
    suspend fun state(): NotificationState

    /** Call once the person has allowed notifications. [serverKey] signs the browser's address. */
    suspend fun turnOn(serverKey: suspend () -> String): PushAddress?

    /** The address that stopped working, for the server to forget. */
    suspend fun turnOff(): PushAddress?

    /** This device's address, while notifications are on. */
    suspend fun address(): PushAddress?
}

/** Asks to show notifications; the answer goes to [onResult]. */
@Composable
expect fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit): () -> Unit

/** A chat a tapped notification asked for, until the app has opened it. */
object ChatLinks {
    val opening = MutableStateFlow<String?>(null)
}
