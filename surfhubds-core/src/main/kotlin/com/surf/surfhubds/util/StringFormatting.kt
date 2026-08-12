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

/**
 * Formata um telefone brasileiro como `(DD) 9XXXX-XXXX` (ou `(DD) XXXX-XXXX` para
 * fixo). Aceita tanto E.164 (`55` + 10/11 dígitos) quanto o número local.
 *
 * O `55` inicial só é tratado como DDI quando a quantidade total de dígitos não
 * cabe em um número local, porque o DDD 55 (RS) também começa com `55` — a
 * checagem é sempre prefixo **e** quantidade de dígitos.
 */
fun String.formatPhoneNumber(): String {
    val local = brazilianLocalDigits(filter(Char::isDigit)) ?: return this
    val ddd = local.take(2)
    val subscriber = local.drop(2)
    return "($ddd) ${subscriber.dropLast(4)}-${subscriber.takeLast(4)}"
}

/**
 * Normaliza para E.164 BR (`55` + DDD + número => 12 ou 13 dígitos) ou devolve
 * `null` quando o valor não é um telefone brasileiro válido.
 *
 * Um número local do DDD 55 (ex.: `55999998888`) começa com `55` sem ter DDI; por
 * isso o prefixo do país é decidido pela quantidade de dígitos, e não pelo prefixo.
 */
fun String.Companion.normalizeBrazilianPhone(raw: String): String? {
    var cleaned = raw.trim().replace(Regex("\\D"), "")
    if (cleaned.isEmpty()) return null
    if (cleaned.startsWith("00")) cleaned = cleaned.dropWhile { it == '0' }
    while (cleaned.startsWith("0")) cleaned = cleaned.drop(1)
    val local = brazilianLocalDigits(cleaned) ?: return null
    return "55$local"
}

/**
 * Reduz uma sequência de dígitos ao número local brasileiro (DDD + 8 ou 9 dígitos),
 * removendo o DDI `55` apenas quando o comprimento comprova que ele é DDI.
 * Devolve `null` para qualquer coisa que não seja telefone brasileiro.
 */
internal fun brazilianLocalDigits(digits: String): String? = when {
    // 10 (DDD + fixo) ou 11 (DDD + celular): já é local, mesmo começando com "55".
    digits.length == 10 || digits.length == 11 -> digits
    // 12 ou 13: só é BR se o "55" extra for realmente DDI.
    (digits.length == 12 || digits.length == 13) && digits.startsWith("55") -> digits.drop(2)
    else -> null
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
