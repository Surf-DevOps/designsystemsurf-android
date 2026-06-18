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
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.AppStrings
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
        // iOS updateDarkModeBorder(): black -> bg secondary, sem borda; dark -> borda 2pt
        // systemGray3; light -> sem borda.
        val scheme = ThemeManager.colorScheme
        val strokeColor: Int?
        val strokeWidth: Float
        if (scheme == ColorScheme.DARK) {
            strokeColor = SYSTEM_GRAY3_DARK
            strokeWidth = 2f
        } else {
            strokeColor = null
            strokeWidth = 0f
        }
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.backgroundSecondary(),
            cornerRadiusDp = 16f,
            strokeColor = strokeColor,
            strokeWidthDp = strokeWidth,
        )
        // iOS: track=primary, knob/texto brancos, seta preta (funciona pq primary é escuro
        // no iOS). Aqui primary pode virar branco no dark/black -> amarramos tudo aos pares
        // que sempre contrastam: knob/texto = contraste do track; seta = cor do track.
        renewButtonSlider.outerColor = DSSColors.primary()
        renewButtonSlider.innerColor = DSSColors.contrastOnPrimary()
        renewButtonSlider.iconColor = DSSColors.primary()
        renewButtonSlider.labelTextColor = DSSColors.contrastOnPrimary()
    }

    private fun setupTree() {
        val width122 = 122f.dpToPx(context)
        val height90 = 90f.dpToPx(context)
        // iOS: contentCard fica 10pt abaixo da validity, e o renewButton 10pt abaixo
        // do contentCard (setupConstraintsCard: constant: 10 em ambos).
        val gap10 = 10f.dpToPx(context)

        // Linha superior: validity (esq) | spacer flexível | data (dir)
        val topRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        topRow.addView(validityView, LayoutParams(width122, height90))
        topRow.addView(View(context), LayoutParams(0, 1, 1f))
        topRow.addView(dataView, LayoutParams(width122, height90))
        addView(topRow, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addView(
            contentCardView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = gap10
            },
        )

        addView(
            renewButtonSlider,
            // 44dp espelha exatamente o XML do flachip-android:
            //   <SlideToActView android:layout_height="44dp"
            //                   app:area_margin="4dp" app:icon_margin="8dp" />
            // Os margins são configurados em DSSSwipeView.
            LayoutParams(LayoutParams.MATCH_PARENT, 44f.dpToPx(context)).apply {
                topMargin = gap10
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
                days <= 2 -> AppStrings.brand(context, "card_plan_recharge_now", "Recarregue agora!")
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
            if (data.payment == PaymentType.PIX) "Pix" else AppStrings.brand(context, "card_plan_credit_card", "Cartão de crédito")
        contentCardView.updatePaymentOptionsUI()
    }

    private fun configureRenewButton(data: CardData) {
        // iOS: bail-out se price não for Int (guard let Int(data.price) else { return }).
        val formattedPrice = data.price.toIntOrNull() ?: return
        // iOS: iFood usa o literal fixo "R$ 25,00"; demais usam Utility.formatPrice
        // (que NÃO inclui o prefixo "R$"). Espaços iguais ao iOS ("  ...  ").
        renewButtonSlider.labelText = if (data.mvno == "iFood") {
            "  Repetir recarga  R$ 25,00"
        } else {
            "  Repetir recarga  ${Utility.formatPrice(formattedPrice)}"
        }
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

    private companion object {
        /** iOS UIColor.systemGray3 (dark) = (72,72,74) — borda do card no modo dark. */
        val SYSTEM_GRAY3_DARK = Color.argb(255, 72, 72, 74)
    }
}
