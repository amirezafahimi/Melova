package app.melova.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
actual fun rememberAudioPicker(onPicked: (LocalFile?) -> Unit): () -> Unit = remember(onPicked) {
    {
        pickerScope.launch {
            val picked: PickedFileJs? = runCatching { MelovaAudioJs.pickAudioFile().await<JsAny?>()?.unsafeCast<PickedFileJs>() }.getOrNull()
            onPicked(
                picked?.let {
                    val dir = LocalFile(cacheDir(WebContext), "picked/${nanoTime()}").apply { mkdirs() }
                    LocalFile(dir, it.name.replace('/', '_')).apply { writeBytes(it.bytes.toByteArray()) }
                }
            )
        }
    }
}
