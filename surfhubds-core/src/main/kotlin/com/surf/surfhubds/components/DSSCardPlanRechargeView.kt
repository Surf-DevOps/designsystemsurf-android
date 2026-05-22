package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSCardPlanRechargeView` do iOS — cartão principal de plano + recarga,
 * com validade (esquerda), data/GB (direita), método de pagamento e swipe-to-renew.
 */
class DSSCardPlanRechargeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    interface Delegate {
        fun didToggleEditingMode(isEditing: Boolean)
        fun paymentTypeSelected(type: PaymentType)
        fun didCompleteSlide(type: PaymentType)
    }

    var delegate: Delegate? = null

    val validityView = DSSValidityView(context)
    val dataView = DSSDataView(context)
    val renewButtonSlider = DSSSwipeView(context).apply {
        sliderCornerRadiusDp = 22f
        thumbnailColor = Color.WHITE
    }
    val contentCardView = DSSContentCardView(context).apply {
        renewButtonSlider = this@DSSCardPlanRechargeView.renewButtonSlider
    }

    private var cardData: CardData? = null
    private var currentPaymentType: PaymentType = PaymentType.CREDIT_CARD

    init {
        clipToOutline = true
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
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = 16f,
            strokeColor = DSSColors.borderDefault(),
            strokeWidthDp = 1f,
        )
    }

    private fun setupTree() {
        val pad24 = 24f.dpToPx(context)
        val pad20 = 20f.dpToPx(context)
        val width122 = 122f.dpToPx(context)
        val height90 = 90f.dpToPx(context)

        // validity em cima-esquerda
        addView(
            validityView,
            LayoutParams(width122, height90).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                leftMargin = pad24
                topMargin = pad20
            },
        )
        // data em cima-direita
        addView(
            dataView,
            LayoutParams(width122, height90).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                rightMargin = pad24
                topMargin = pad20
            },
        )
        // content card abaixo
        addView(
            contentCardView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.TOP
                leftMargin = pad24
                rightMargin = pad24
                topMargin = pad20 + height90 + 10f.dpToPx(context)
            },
        )
        // renew slider no rodapé
        addView(
            renewButtonSlider,
            LayoutParams(LayoutParams.MATCH_PARENT, 44f.dpToPx(context)).apply {
                gravity = android.view.Gravity.BOTTOM
                leftMargin = pad24
                rightMargin = pad24
                bottomMargin = 10f.dpToPx(context)
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

        validityView.daysLabel.text = "${data.validityDays} dias"
        validityView.validUntilLabel.text = "válido até ${data.planDate}"

        dataView.gigasLabel.text = "${data.available}GB"
        dataView.availableLabel.text = "disponível ${data.totalValue}GB"
        if (data.totalValue > 0) {
            val used = data.totalValue - data.availableValue
            dataView.progressDataView.progress = ((used.toFloat() / data.totalValue) * 100).toInt()
        }

        contentCardView.selectedPaymentType = data.payment
        contentCardView.typeCardLabel.text = if (data.payment == PaymentType.PIX) "Pix" else "Cartão de crédito"

        val priceLabel = if (data.mvno == "iFood") {
            "  Repetir recarga  R$ 25,00"
        } else {
            val intPrice = data.price.toIntOrNull() ?: 0
            "  Repetir recarga  R$ %d,00".format(intPrice)
        }
        renewButtonSlider.textLabel.text = priceLabel
    }

    fun resetSlider() {
        renewButtonSlider.resetState(animated = true)
    }
}
