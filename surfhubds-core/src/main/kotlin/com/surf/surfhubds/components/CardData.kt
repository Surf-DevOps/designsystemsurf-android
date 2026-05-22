package com.surf.surfhubds.components

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
)
