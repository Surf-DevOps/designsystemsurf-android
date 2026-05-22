package com.surf.surfhubds.components

import kotlin.math.roundToInt

/**
 * Port do `ConvertData` (singleton iOS). Helpers para converter MB/GB,
 * calcular porcentagens e formatar telefones em padrão internacional brasileiro.
 */
object ConvertData {

    enum class ConversionType { MB_TO_GB, GB_TO_MB }

    /** Converte [value] entre MB/GB. Retorna `null` quando o tipo não é Double/Int. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Number> convertDataSize(value: T, conversion: ConversionType): T? {
        return when (conversion) {
            ConversionType.MB_TO_GB -> when (value) {
                is Double -> (value / 1000.0) as? T
                is Int -> (value / 1000) as? T
                else -> null
            }
            ConversionType.GB_TO_MB -> when (value) {
                is Double -> (value * 1000.0).roundToInt().toDouble() as? T
                is Int -> (value * 1000) as? T
                else -> null
            }
        }
    }

    /** Retorna a porcentagem usada (0..100). */
    fun calculateUsedPercentage(used: Double, total: Int): Int {
        if (total <= 0) return 0
        return ((used / total.toDouble()) * 100).toInt()
    }

    /** "11 99999-9999" → "5511999999999". */
    fun formatPhoneNumberToInternational(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() }
        if (digits.length < 10) return ""
        return "55$digits"
    }
}
