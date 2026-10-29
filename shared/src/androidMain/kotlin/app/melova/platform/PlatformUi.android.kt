package app.melova.platform

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File

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

/** Copies a picked document (content://) into the cache under its display name. */
private fun copyPicked(context: android.content.Context, uri: Uri): LocalFile {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?.takeIf { it.isNotBlank() }
        ?: "audio"
    val dir = File(context.cacheDir, "picked/${System.nanoTime()}").apply { mkdirs() }
    val target = File(dir, name.replace('/', '_'))
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { input.copyTo(it) }
    } ?: error("picked file could not be opened")
    return target.toLocalFile()
}
