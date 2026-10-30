package app.melova.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.melova.audio.web.GoogleAnswerJs
import app.melova.audio.web.MelovaAudioJs
import app.melova.audio.web.PickedFileJs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

/** The browser asks for the microphone itself the first time a recording starts. */
@Composable
actual fun rememberMicPermission(onResult: (granted: Boolean) -> Unit): MicPermission = remember {
    object : MicPermission {
        override fun isGranted(): Boolean = true
        override fun request() = onResult(true)
    }
}

private val pickerScope: CoroutineScope = MainScope()

@Composable
actual fun rememberAudioPicker(onPicked: (LocalFile?) -> Unit): () -> Unit =
    remember(onPicked) { { pickFile("audio/*", onPicked) } }

/** Named types rather than any image, so iOS hands over a JPEG instead of a HEIC. */
@Composable
actual fun rememberImagePicker(onPicked: (LocalFile?) -> Unit): () -> Unit =
    remember(onPicked) { { pickFile("image/jpeg,image/png,image/webp", onPicked) } }

private fun pickFile(accept: String, onPicked: (LocalFile?) -> Unit) {
    pickerScope.launch {
        val picked: PickedFileJs? = runCatching { MelovaAudioJs.pickFile(accept).await<JsAny?>()?.unsafeCast<PickedFileJs>() }.getOrNull()
        onPicked(
            picked?.let {
                val dir = LocalFile(cacheDir(WebContext), "picked/${nanoTime()}").apply { mkdirs() }
                LocalFile(dir, it.name.replace('/', '_')).apply { writeBytes(it.bytes.toByteArray()) }
            }
        )
    }
}

@Composable
actual fun rememberGoogleSignIn(onResult: (GoogleSignInResult) -> Unit): (clientId: String) -> Unit =
    remember(onResult) {
        { clientId ->
            pickerScope.launch {
                val answer = runCatching { MelovaAudioJs.googleSignIn(clientId).await<JsAny?>()?.unsafeCast<GoogleAnswerJs>() }
                    .getOrNull()
                onResult(
                    when {
                        answer == null -> GoogleSignInResult.Failed("ورود با گوگل ممکن نشد. با شماره موبایل یا ایمیل وارد شوید.")
                        answer.cancelled -> GoogleSignInResult.Cancelled
                        else -> answer.credential?.let(GoogleSignInResult::Token)
                            ?: GoogleSignInResult.Failed("ورود با گوگل ممکن نشد. با شماره موبایل یا ایمیل وارد شوید.")
                    }
                )
            }
        }
    }

@Composable
actual fun rememberInstallOffer(): (() -> Unit)? {
    var isOffered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isOffered = MelovaAudioJs.canInstall()
        MelovaAudioJs.watchInstall { isOffered = MelovaAudioJs.canInstall() }
    }
    return if (isOffered) MelovaAudioJs::install else null
}
