package com.surf.surfhubds.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DateHelpers
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.Utility
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSCardPlanRechargeView` do iOS — cartão principal de plano + recarga,
 * com validade (esquerda), data/GB (direita), método de pagamento e slide-to-renew.
 *
 * Layout vertical: o slider acompanha o crescimento do bloco de método de pagamento
 * quando o usuário toca em "Alterar" (passa de 1 linha pra 2 linhas).
 */
class DSSCardPlanRechargeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    interface Delegate {
        fun didToggleEditingMode(isEditing: Boolean)
        fun paymentTypeSelected(type: PaymentType)
        fun didCompleteSlide(type: PaymentType)
    }

    var delegate: Delegate? = null

    val validityView = DSSValidityView(context)
    val dataView = DSSDataView(context)
    val renewButtonSlider = DSSSwipeView(context)
    val contentCardView = DSSContentCardView(context).apply {
        renewButtonSlider = this@DSSCardPlanRechargeView.renewButtonSlider
    }

    private var cardData: CardData? = null
    private var currentPaymentType: PaymentType = PaymentType.CREDIT_CARD

    init {
        orientation = VERTICAL
        clipToOutline = true
        setPadding(
            24f.dpToPx(context),
            20f.dpToPx(context),
            24f.dpToPx(context),
            10f.dpToPx(context),
        )
        setupTree()
        contentCardView.delegate = object : DSSContentCardView.Delegate {
            override fun didToggleEditingMode(isEditing: Boolean) {
                delegate?.didToggleEditingMode(isEditing)
            }
            override fun paymentTypeSelected(type: Int) {
                currentPaymentType = if (type == 0) PaymentType.CREDIT_CARD else PaymentType.PIX
                delegate?.paymentTypeSelected(currentPaymentType)
            }
        }
        renewButtonSlider.onCompleted = {
            delegate?.didCompleteSlide(currentPaymentType)
        }
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.backgroundSecondary(),
            cornerRadiusDp = 16f,
        )
        renewButtonSlider.outerColor = DSSColors.primary()
        renewButtonSlider.innerColor = Color.WHITE
        renewButtonSlider.iconColor = Color.BLACK
        renewButtonSlider.labelTextColor = Color.WHITE
    }

    private fun setupTree() {
        val width122 = 122f.dpToPx(context)
        val height90 = 90f.dpToPx(context)
        val gap16 = 16f.dpToPx(context)

        // Linha superior: validity (esq) | spacer flexível | data (dir)
        val topRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        topRow.addView(validityView, LayoutParams(width122, height90))
        topRow.addView(View(context), LayoutParams(0, 1, 1f))
        topRow.addView(dataView, LayoutParams(width122, height90))
        addView(topRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addView(
            contentCardView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = gap16
            },
        )

        addView(
            renewButtonSlider,
            LayoutParams(LayoutParams.MATCH_PARENT, 52f.dpToPx(context)).apply {
                topMargin = gap16
                gravity = Gravity.BOTTOM
            },
        )
    }

    /** Configura via parâmetros nomeados (mesmo método de iOS). */
    fun configure(
        planDate: String,
        available: Int,
        price: String,
        validityDays: Int,
        mvno: String? = null,
        payment: PaymentType = PaymentType.CREDIT_CARD,
        totalValue: Int,
        availableValue: Int,
    ) {
        val data = CardData(
            planDate = planDate,
            available = available,
            price = price,
            validityDays = validityDays,
            mvno = mvno,
            payment = payment,
            totalValue = totalValue,
            availableValue = availableValue,
        )
        configure(data)
    }

    fun configure(data: CardData) {
        this.cardData = data
        this.currentPaymentType = data.payment

        configureValidity(data)
        configureDataUsage(data)
        configurePayment(data)
        configureRenewButton(data)

        requestLayout()
    }

    private fun configureValidity(data: CardData) {
        val start = DateHelpers.parseGatewayDate(data.planDate)
        val end = start?.let { DateHelpers.addDays(it, data.validityDays) }

        if (end != null) {
            val days = DateHelpers.daysRemaining(end)
            validityView.daysLabel.text = "$days dias"
            validityView.validUntilLabel.text = when {
                days <= 2 -> "Recarregue agora!"
                days <= 5 -> "vence em $days dias"
                else -> "válido até ${DateHelpers.formatDDMM(end)}"
            }
            val totalDays = data.validityDays.coerceAtLeast(1)
            val used = (totalDays - days.coerceAtLeast(0)).coerceAtLeast(0)
            validityView.progressDaysView.progress = ((used.toFloat() / totalDays) * 100).toInt()
            applyProgressTint(validityView.progressDaysView, progressColorForDays(days))
        } else {
            validityView.daysLabel.text = "${data.validityDays} dias"
            validityView.validUntilLabel.text = "válido até ${data.planDate}"
        }
    }

    private fun configureDataUsage(data: CardData) {
        dataView.gigasLabel.text = "${data.available}GB"
        dataView.availableLabel.text = "disponível ${data.totalValue}GB"
        if (data.totalValue > 0) {
            val used = (data.totalValue - data.availableValue).toFloat()
            val usedRatio = (used / data.totalValue.toFloat()).coerceIn(0f, 1f)
            dataView.progressDataView.progress = (usedRatio * 100).toInt()
            applyProgressTint(dataView.progressDataView, progressColorForUsage(usedRatio))
        }
    }

    /**
     * Tinge apenas a camada `android.R.id.progress` (espelha `progressTintColor` do
     * iOS). O `setColorFilter` no drawable inteiro tinge também a track, sumindo o
     * contraste.
     */
    private fun applyProgressTint(progressBar: ProgressBar, color: Int) {
        progressBar.progressTintList = ColorStateList.valueOf(color)
        (progressBar.progressDrawable as? LayerDrawable)
            ?.findDrawableByLayerId(android.R.id.progress)
            ?.setTint(color)
    }

    private fun configurePayment(data: CardData) {
        contentCardView.selectedPaymentType = data.payment
        contentCardView.typeCardLabel.text =
            if (data.payment == PaymentType.PIX) "Pix" else "Cartão de crédito"
        contentCardView.updatePaymentOptionsUI()
    }

    private fun configureRenewButton(data: CardData) {
        // Valor vem em centavos (3000 → "30,00"). Espelha `Utility.formatPrice` do iOS.
        val cents = when {
            data.mvno == "iFood" -> 2500
            else -> data.price.toIntOrNull() ?: 0
        }
        renewButtonSlider.labelText = "Repetir recarga  ${Utility.formatPrice(cents)}"
    }

    private fun progressColorForDays(days: Int): Int = when {
        days >= 21 -> Color.parseColor("#34C759")
        days in 11..20 -> Color.parseColor("#FFCC00")
        else -> Color.parseColor("#FF3B30")
    }

    private fun progressColorForUsage(ratio: Float): Int = when {
        ratio < 0.5f -> Color.parseColor("#34C759")
        ratio < 0.7f -> Color.parseColor("#FFCC00")
        else -> Color.parseColor("#FF3B30")
    }

    fun resetSlider() {
        renewButtonSlider.resetState(animated = true)
    }
}
