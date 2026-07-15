package com.surf.surfhubds.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Port das extensions de `ViewUtils.swift` (formatPhoneNumber, formatToBRL, formatDate, etc).
 *
 * Em Kotlin viram extension functions em [String].
 */

private val brLocale = Locale("pt", "BR")

enum class DateFormatType { ONLY_DATE, DATE_AND_TIME, SHORT_MONTH }

fun String.formatPhoneNumber(): String {
    if (!startsWith("55")) return this
    val trimmed = drop(2)
    if (trimmed.length != 11) return this
    val ddd = trimmed.take(2)
    val prefix = trimmed.drop(2).take(5)
    val suffix = trimmed.takeLast(4)
    return "($ddd) $prefix-$suffix"
}

fun String.Companion.normalizeBrazilianPhone(raw: String): String? {
    var cleaned = raw.trim().replace(Regex("\\D"), "")
    if (cleaned.isEmpty()) return null
    if (cleaned.startsWith("00")) cleaned = cleaned.dropWhile { it == '0' }
    while (cleaned.startsWith("0")) cleaned = cleaned.drop(1)
    if (!cleaned.startsWith("55")) cleaned = "55$cleaned"
    return if (cleaned.length in 12..13) cleaned else null
}

fun String.formatDate(type: DateFormatType): String {
    val date: Date = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).parse(this) ?: return ""
    } catch (e: Exception) {
        try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(this) ?: return "" }
        catch (e: Exception) { return "" }
    }
    val pattern = when (type) {
        DateFormatType.ONLY_DATE -> "dd/MM/yyyy"
        DateFormatType.DATE_AND_TIME -> "dd/MM/yyyy HH:mm"
        DateFormatType.SHORT_MONTH -> "dd MMM yyyy"
    }
    return SimpleDateFormat(pattern, brLocale).format(date).lowercase(brLocale)
}

fun String.formatToBRLCents(): String {
    val cents = toIntOrNull() ?: 0
    return formatBRL(cents / 100.0)
}

fun String.formatToBRL(): String {
    val normalized = replace(",", ".")
    val value = normalized.toDoubleOrNull() ?: return this
    return formatBRL(value)
}

/** 12345 -> "12.345" (agrupamento pt_BR, sem casas decimais). */
fun Int.formatPoints(): String {
    val fmt = NumberFormat.getIntegerInstance(brLocale)
    return fmt.format(this)
}

/** 12.5 -> "R$ 12,50". */
fun Double.formatToBRL(): String = formatBRL(this)

private fun formatBRL(value: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(brLocale)
    return fmt.format(value)
}
