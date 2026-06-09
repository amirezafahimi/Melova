package app.melova.data.remote

import java.io.File
import okhttp3.ResponseBody

internal fun ResponseBody.writeAtomically(target: File, tempDir: File, minBytes: Long = 1L) {
    val temp = File(tempDir, "melova_dl_${System.nanoTime()}.tmp")
    try {
        byteStream().use { input ->
            temp.outputStream().buffered().use { output -> input.copyTo(output) }
        }
        if (!temp.exists() || temp.length() < minBytes) {
            error("empty download")
        }
        temp.copyTo(target, overwrite = true)
    } finally {
        temp.delete()
    }
}
