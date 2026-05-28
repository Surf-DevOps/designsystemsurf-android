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

    fun daysRemaining(until: Date): Int {
        val diff = until.time - Date().time
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    fun formatDDMM(date: Date): String =
        SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(date)
}
