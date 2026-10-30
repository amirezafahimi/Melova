package app.melova.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.io.File
import kotlinx.coroutines.launch

@Composable
actual fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): MicPermission {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return remember(context, launcher) {
        object : MicPermission {
            override fun isGranted(): Boolean =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED

            override fun request() = launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
actual fun rememberAudioPicker(onPicked: (LocalFile?) -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onPicked(uri?.let { runCatching { copyPicked(context, it) }.getOrNull() })
    }
    return remember(launcher) { { launcher.launch("audio/*") } }
}

@Composable
actual fun rememberImagePicker(onPicked: (LocalFile?) -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onPicked(uri?.let { runCatching { copyPicked(context, it, fallbackName = "photo") }.getOrNull() })
    }
    return remember(launcher) { { launcher.launch("image/*") } }
}

@Composable
actual fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): (clientId: String) -> Unit {
    // The activity, not the application: the account chooser opens on top of it.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope, onResult) {
        { clientId -> scope.launch { onResult(requestGoogleIdToken(context, clientId)) } }
    }
}

private suspend fun requestGoogleIdToken(context: Context, clientId: String): GoogleSignInResult = try {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
        .build()
    val credential = CredentialManager.create(context).getCredential(context, request).credential
    GoogleSignInResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
} catch (_: GetCredentialCancellationException) {
    GoogleSignInResult.Cancelled
} catch (_: GetCredentialException) {
    GoogleSignInResult.Failed("ورود با گوگل روی این گوشی ممکن نشد. با شماره موبایل یا ایمیل وارد شوید.")
} catch (_: GoogleIdTokenParsingException) {
    GoogleSignInResult.Failed("ورود با گوگل ممکن نشد. با شماره موبایل یا ایمیل وارد شوید.")
}

/** Copies a picked document (content://) into the cache under its display name. */
private fun copyPicked(context: Context, uri: Uri, fallbackName: String = "audio"): LocalFile {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?.takeIf { it.isNotBlank() }
        ?: fallbackName
    val dir = File(context.cacheDir, "picked/${System.nanoTime()}").apply { mkdirs() }
    val target = File(dir, name.replace('/', '_'))
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { input.copyTo(it) }
    } ?: error("picked file could not be opened")
    return target.toLocalFile()
}
