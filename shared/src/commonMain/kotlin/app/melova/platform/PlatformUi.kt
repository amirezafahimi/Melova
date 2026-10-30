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

/** Like [rememberAudioPicker], for a photo. */
@Composable
expect fun rememberImagePicker(onPicked: (LocalFile?) -> Unit): () -> Unit

sealed interface GoogleSignInResult {
    data class Token(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failed(val message: String) : GoogleSignInResult
}

/** Asks Google for an ID token for this client id; the server checks it and signs the student in. */
@Composable
expect fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): (clientId: String) -> Unit

/** Installs the web app, while the browser offers that; null otherwise, as in the Android app. */
@Composable
expect fun rememberInstallOffer(): (() -> Unit)?
