package com.surf.surfhubds.components

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/**
 * Port do `PaymentType` do iOS — método de pagamento usado em DSSCardPlanRechargeView /
 * DSSCardNoInternetView.
 */
enum class PaymentType { PIX, CREDIT_CARD }

/**
 * Port do `CardData` do iOS — payload do plano para [DSSCardPlanRechargeView].
 */
data class CardData(
    val planDate: String,
    val available: Int,
    val price: String,
    val validityDays: Int,
    val mvno: String? = null,
    val payment: PaymentType = PaymentType.PIX,
    val totalValue: Int,
    val availableValue: Int,
    val internetUsage: PlanInternetUsage = PlanInternetUsage(
        totalValue.toDouble() * 1000,
        availableValue.toDouble() * 1000,
    ),
) {
    /** Valores em MB da consulta consolidada, sem truncar GB antes da apresentação. */
    constructor(
        planDate: String,
        price: String,
        validityDays: Int,
        mvno: String? = null,
        payment: PaymentType = PaymentType.PIX,
        totalDataInMB: Int,
        availableDataInMB: Int,
    ) : this(
        planDate = planDate,
        available = availableDataInMB / 1000,
        price = price,
        validityDays = validityDays,
        mvno = mvno,
        payment = payment,
        totalValue = totalDataInMB / 1000,
        availableValue = availableDataInMB / 1000,
        internetUsage = PlanInternetUsage(totalDataInMB.toDouble(), availableDataInMB.toDouble()),
    )
}

/** Port do `PlanInternetUsage` do iOS (MR de correção do consumo na Home). */
data class PlanInternetUsage(private val rawTotalMB: Double, private val rawAvailableMB: Double) {
    val totalMB: Double = if (rawTotalMB.isFinite()) max(0.0, rawTotalMB) else 0.0
    val availableMB: Double = if (rawAvailableMB.isFinite()) rawAvailableMB.coerceIn(0.0, totalMB) else 0.0
    val usedMB: Double get() = totalMB - availableMB
    val usedFraction: Float get() = if (totalMB > 0) (usedMB / totalMB).toFloat() else 0f

    // Mesmo arredondamento de Utility.formatMBToGBWithDecimal usado pela aba Consumo.
    val usedGBText: String get() = String.format(Locale.US, "%.1f", ceil(usedMB / 100) / 10)
    val availableGBText: String get() = String.format(Locale.US, "%.1f", ceil(availableMB / 100) / 10)
}
