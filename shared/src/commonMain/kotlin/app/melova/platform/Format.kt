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

/** Only the digits of [text], with Persian and Arabic ones turned into 0-9. */
fun digitsOnly(text: String): String = text.mapNotNull { ch ->
    when (ch) {
        in '0'..'9' -> ch
        in '۰'..'۹' -> '0' + (ch - '۰')
        in '٠'..'٩' -> '0' + (ch - '٠')
        else -> null
    }
}.joinToString("")

/** Two digits, like "%02d". */
fun Number.pad2(): String = toLong().toString().padStart(2, '0')

/** An integer in Persian digits with the Persian thousands separator, as fa-IR formats it. */
fun formatPersianInteger(amount: Long): String {
    val digits = abs(amount).toString().reversed().chunked(3).joinToString("٬").reversed()
    val persian = persianDigits(digits)
    return if (amount < 0) "-$persian" else persian
}

fun persianDigits(text: String): String =
    text.map { ch -> if (ch in '0'..'9') '۰' + (ch - '0') else ch }.joinToString("")

private const val MINUTES_IN_A_DAY = 24 * 60L

private fun localMinutes(timeMs: Long): Long = timeMs / 60_000 + utcOffsetMinutes(timeMs)

/** The calendar day [timeMs] falls on by this device's clock, counted from 1970. */
fun localDay(timeMs: Long): Long = localMinutes(timeMs) / MINUTES_IN_A_DAY

/** The hour and minute on this device's clock: «۱۴:۳۵». */
fun formatTimeOfDay(timeMs: Long): String {
    val minuteOfDay = localMinutes(timeMs) % MINUTES_IN_A_DAY
    return persianDigits("${(minuteOfDay / 60).pad2()}:${(minuteOfDay % 60).pad2()}")
}

/** When a message came, on this device's clock: «۱۴:۳۵», «دیروز ۱۴:۳۵», «۳ روز پیش». */
fun formatMessageTime(timeMs: Long, nowMs: Long = currentTimeMillis()): String {
    val days = localDay(nowMs) - localDay(timeMs)
    val clock = formatTimeOfDay(timeMs)
    return when {
        days <= 0L -> clock
        days == 1L -> "دیروز $clock"
        else -> "${formatPersianInteger(days)} روز پیش"
    }
}

/** The heading over a day's messages: «امروز», «دیروز» or a solar date like «۹/۸/۱۴۰۵». */
fun formatMessageDay(timeMs: Long, nowMs: Long = currentTimeMillis()): String {
    val day = localDay(timeMs)
    return when (localDay(nowMs) - day) {
        0L -> "امروز"
        1L -> "دیروز"
        else -> formatSolarDate(day)
    }
}

private const val DAYS_IN_33_YEARS = 12_053L
private const val DAYS_IN_4_YEARS = 1_461L
private const val DAYS_IN_A_YEAR = 365L
private const val DAYS_IN_FIRST_SIX_MONTHS = 186L

/** Where the count below starts, as days before 1970 and the solar year of that first day. */
private const val DAYS_BEFORE_1970 = 1_075_195L
private const val FIRST_SOLAR_YEAR = -1_595L

/** [day], counted from 1970, as a solar date: day/month/year in Persian digits. */
internal fun formatSolarDate(day: Long): String {
    var days = day + DAYS_BEFORE_1970
    var year = FIRST_SOLAR_YEAR + 33 * (days / DAYS_IN_33_YEARS)
    days %= DAYS_IN_33_YEARS
    year += 4 * (days / DAYS_IN_4_YEARS)
    days %= DAYS_IN_4_YEARS
    if (days > DAYS_IN_A_YEAR) {
        year += (days - 1) / DAYS_IN_A_YEAR
        days = (days - 1) % DAYS_IN_A_YEAR
    }
    val isFirstHalf = days < DAYS_IN_FIRST_SIX_MONTHS
    val dayOfHalf = if (isFirstHalf) days else days - DAYS_IN_FIRST_SIX_MONTHS
    val monthLength = if (isFirstHalf) 31 else 30
    val month = dayOfHalf / monthLength + if (isFirstHalf) 1 else 7
    return persianDigits("${dayOfHalf % monthLength + 1}/$month/$year")
}
