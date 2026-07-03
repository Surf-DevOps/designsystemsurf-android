package com.surf.surfhubds.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Equivalente ao `Date+SurfHubDS` do iOS — parse de datas no formato do gateway,
 * cálculo de dias restantes e formatação `dd/MM`. Usado pelo
 * [com.surf.surfhubds.components.DSSCardPlanRechargeView].
 */
internal object DateHelpers {

    private val GATEWAY_FORMATS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "dd/MM/yyyy",
    )

    fun parseGatewayDate(s: String?): Date? {
        if (s.isNullOrBlank()) return null
        for (pattern in GATEWAY_FORMATS) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                return fmt.parse(s) ?: continue
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun addDays(date: Date, days: Int): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.time
    }

    /**
     * Dias restantes até [until] a partir de agora, clampados em >= 0
     * (espelha o `max(0, ...)` do `Date.daysRemaining(until:)` do iOS).
     */
    fun daysRemaining(until: Date): Int {
        val diff = until.time - Date().time
        return maxOf(0, TimeUnit.MILLISECONDS.toDays(diff).toInt())
    }

    fun formatDDMM(date: Date): String =
        SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(date)

    /**
     * Espelha `Date.progress(in:)` do iOS — fração consumida de um período de
     * [totalDays] dias, calculada a partir dos dias restantes até [date] (a partir
     * de agora). Retorna `(totalDays - restantes) / totalDays` como [Float].
     *
     * iOS: `Float(totalDays - remaining) / Float(totalDays)`.
     */
    fun progress(date: Date, totalDays: Int): Float {
        val remaining = daysRemaining(date)
        return (totalDays - remaining).toFloat() / totalDays.toFloat()
    }

    /**
     * Espelha `Date.formatted(_:locale:timeZone:)` do iOS — formatação genérica
     * com [pattern] (default `dd/MM`), [locale] (default pt-BR) e [zone]
     * (default fuso atual do dispositivo).
     */
    fun formatted(
        date: Date,
        pattern: String = "dd/MM",
        locale: Locale = Locale("pt", "BR"),
        zone: TimeZone = TimeZone.getDefault(),
    ): String {
        val fmt = SimpleDateFormat(pattern, locale)
        fmt.timeZone = zone
        return fmt.format(date)
    }
}
