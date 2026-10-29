package app.melova.data.remote

import app.melova.platform.LocalFile

/** Writes a downloaded body to [target] only once it is complete and at least [minBytes]. */
internal fun ByteArray.writeAtomically(target: LocalFile, minBytes: Long = 1L) {
    if (size < minBytes) error("empty download")
    target.writeBytes(this)
}
