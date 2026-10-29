package app.melova.platform

import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

fun Int8Array.toByteArray(): ByteArray = ByteArray(length) { this[it] }

fun ByteArray.toInt8Array(): Int8Array = Int8Array(size).also { array ->
    for (index in indices) array[index] = this[index]
}
