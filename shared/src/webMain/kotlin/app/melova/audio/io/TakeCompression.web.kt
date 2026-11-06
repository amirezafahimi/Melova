package app.melova.audio.io

import app.melova.audio.web.MelovaAudioJs
import app.melova.platform.toByteArray
import app.melova.platform.toInt8Array
import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array

internal actual suspend fun compressedTake(wav: ByteArray): CompressedTake? =
    runCatching { MelovaAudioJs.compressTake(wav.toInt8Array(), TAKE_BIT_RATE).await<JsAny?>() }
        .getOrNull()
        ?.unsafeCast<Int8Array>()
        ?.let { CompressedTake(it.toByteArray(), "aac", "audio/aac") }
