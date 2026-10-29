package app.melova.platform

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** [value] with exactly [decimals] digits after the point, like "%.Nf". */
fun formatFixed(value: Double, decimals: Int): String {
    val scale = 10.0.pow(decimals)
    val scaled = (abs(value) * scale).roundToLong()
    val whole = scaled / scale.toLong()
    val sign = if (value < 0 && scaled != 0L) "-" else ""
    if (decimals == 0) return "$sign$whole"
    val fraction = (scaled % scale.toLong()).toString().padStart(decimals, '0')
    return "$sign$whole.$fraction"
}

fun formatFixed(value: Float, decimals: Int): String = formatFixed(value.toDouble(), decimals)

/** Two digits, like "%02d". */
fun Number.pad2(): String = toLong().toString().padStart(2, '0')

/** An integer in Persian digits with the Persian thousands separator, as fa-IR formats it. */
fun formatPersianInteger(amount: Long): String {
    val digits = abs(amount).toString().reversed().chunked(3).joinToString("٬").reversed()
    val persian = digits.map { ch -> if (ch in '0'..'9') '۰' + (ch - '0') else ch }.joinToString("")
    return if (amount < 0) "-$persian" else persian
}
