package com.surf.surfhubds.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Port do `Utility.swift` do iOS — utilitários de data, CPF/CEP/moeda, QRCode e consumo.
 */
object Utility {

    private val brLocale = Locale("pt", "BR")
    private val brZone: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun remainingDays(planDateISO: String, expirationDateISO: String, now: Date = Date()): Int {
        val expiration = parseISO8601(expirationDateISO) ?: return 0
        val today = LocalDate.ofInstant(now.toInstant(), brZone)
        val target = LocalDate.ofInstant(expiration.toInstant(), brZone)
        val days = ChronoUnit.DAYS.between(today, target).toInt()
        return max(days, 0)
    }

    fun getTodayInBrazilianFormat(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", brLocale)
        return fmt.format(Date())
    }

    fun formatCEP(text: String): String {
        val digits = text.filter { it.isDigit() }.take(8)
        return if (digits.length <= 5) digits else "${digits.take(5)}-${digits.drop(5)}"
    }

    fun currentDatePlus6Hours(): String = date(Date(), addingHours = 6)

    fun date(date: Date, addingHours: Int): String {
        val cal = Calendar.getInstance().apply {
            time = date
            add(Calendar.HOUR_OF_DAY, addingHours)
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        return fmt.format(cal.time)
    }

    fun generateQRCode(content: String, sizePx: Int = 512): Bitmap? = try {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) for (y in 0 until sizePx) {
            bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }
        bmp
    } catch (e: Exception) { null }

    fun formatMBToGB(mbValue: String): String {
        val mb = mbValue.trim().toDoubleOrNull() ?: return mbValue
        return (mb / 1000.0).roundToInt().toString()
    }

    fun formatMBToGBWithDecimal(mbValue: Int): String {
        val gb = mbValue / 1000.0
        val rounded = ceil(gb * 10.0) / 10.0
        return String.format(brLocale, "%.1f", rounded)
    }

    fun formatDateToBrazilianFormat(iso8601String: String): String {
        val date = parseISO8601(iso8601String) ?: return iso8601String
        val fmt = SimpleDateFormat("dd MMM yyyy", brLocale)
        return fmt.format(date)
    }

    fun formatDateToBrazilianFormatPlus30Days(iso8601String: String): String {
        val date = parseISO8601(iso8601String) ?: return iso8601String
        val cal = Calendar.getInstance().apply { time = date; add(Calendar.DAY_OF_MONTH, 30) }
        val fmt = SimpleDateFormat("dd MMM yyyy", brLocale)
        return fmt.format(cal.time)
    }

    fun formatDateToBrazilianFormatInverted(iso8601String: String): String {
        val date = parseISO8601(iso8601String) ?: return iso8601String
        return SimpleDateFormat("yyyy-MM-dd", brLocale).format(date)
    }

    fun formatPrice(priceInCents: Int): String {
        val reais = priceInCents / 100.0
        val fmt = NumberFormat.getNumberInstance(brLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return fmt.format(reais)
    }

    fun formatDateToDDMM(iso8601String: String): String {
        val date = parseISO8601(iso8601String) ?: return ""
        return SimpleDateFormat("dd/MM", brLocale).format(date)
    }

    fun calculateDataConsumptionPercentage(qtDadoAtribuido: Int, consumoDado: Int): Int {
        if (qtDadoAtribuido <= 0) return 0
        val safeRemaining = max(0, min(consumoDado, qtDadoAtribuido))
        val consumed = qtDadoAtribuido - safeRemaining
        return min(100, ((consumed.toDouble() / qtDadoAtribuido) * 100.0).roundToInt())
    }

    fun calculateDataRemainingPercentage(qtDadoAtribuido: Int, consumoDado: Int): Int {
        if (qtDadoAtribuido <= 0) return 0
        val safe = max(0, consumoDado)
        val remaining = max(0, qtDadoAtribuido - safe)
        return min(100, ((remaining.toDouble() / qtDadoAtribuido) * 100.0).roundToInt())
    }

    fun extractBandPlanName(noPlano: String): String {
        val prefix = "BAND SPORTS MOBILE "
        val trimmed = noPlano.trim()
        return if (trimmed.startsWith(prefix)) trimmed.drop(prefix.length).trim() else trimmed
    }

    fun shortenedPlanName(noPlano: String, mvnoName: String): String {
        val upper = noPlano.uppercase(brLocale)
        if (!upper.contains("CONTROLE")) return noPlano
        val brand = mvnoName.split('-', ' ').firstOrNull()?.uppercase(brLocale) ?: mvnoName.uppercase(brLocale)
        return when {
            upper.contains("TRIMESTRAL") || upper.contains(" TRI") -> "$brand CONTROLE TRI"
            upper.contains("SEMESTRAL") || upper.contains(" SEM") -> "$brand CONTROLE SEM"
            upper.contains("ANUAL") -> "$brand CONTROLE ANUAL"
            upper.contains("MENSAL") -> "$brand CONTROLE MENSAL"
            else -> "$brand CONTROLE"
        }
    }

    fun formatCPF(cpf: String): String {
        val digits = cpf.filter { it.isDigit() }.take(11)
        return buildString {
            digits.forEachIndexed { i, c ->
                if (i == 3 || i == 6) append('.')
                if (i == 9) append('-')
                append(c)
            }
        }
    }

    private fun parseISO8601(s: String): Date? = try {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        fmt.parse(s)
    } catch (e: Exception) {
        try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(s) }
        catch (e: Exception) { try { Date.from(Instant.parse(s)) } catch (e: Exception) { null } }
    }
}
