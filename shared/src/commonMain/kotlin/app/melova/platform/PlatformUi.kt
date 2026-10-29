package app.melova.platform

import androidx.compose.runtime.Composable

/** Asks for the microphone; on the web the browser asks when recording starts. */
interface MicPermission {
    fun isGranted(): Boolean
    fun request()
}

@Composable
expect fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): MicPermission

/**
 * Opens the system picker for an audio file and copies the choice into the app's cache, keeping
 * its display name, so the rest of the app only deals with [LocalFile]. Null when cancelled.
 */
@Composable
expect fun rememberAudioPicker(onPicked: (LocalFile?) -> Unit): () -> Unit
