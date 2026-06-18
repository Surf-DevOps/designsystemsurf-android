package com.surf.surfhubds.components

import android.content.Context
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSContentCardView` do iOS — bloco interno do [DSSCardPlanRechargeView]
 * com o método de pagamento e seleção pix / cartão.
 */
class DSSContentCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Bridge para o pai DSSCardPlanRechargeView. */
    interface Delegate {
        fun didToggleEditingMode(isEditing: Boolean)
        fun paymentTypeSelected(type: Int)
    }

    var delegate: Delegate? = null
    var renewButtonSlider: DSSSwipeView? = null

    var isEditingPaymentMethod: Boolean = false
        private set

    var selectedPaymentType: PaymentType = PaymentType.CREDIT_CARD

    /**
     * Resolver de drawables por método (ilMaster / pix). Por padrão usa o
     * [ImageLoader] do core resolvendo pela brand atual.
     */
    var iconResolver: (PaymentType) -> android.graphics.drawable.Drawable? = { type ->
        val name = if (type == PaymentType.PIX) "pix" else "ilMaster"
        ImageLoader.image(context, name)
    }

    val typeCardLabel = TextView(context).apply {
        text = AppStrings.brand(context, "card_plan_credit_card", "Cartão de crédito")
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
    }

    init {
        orientation = VERTICAL
        // iOS: contentCard reserva `bottomSpacing = 15` abaixo das opções de pagamento
        // (updatePaymentOptionsUI). Replicado como padding inferior.
        setPadding(0, 0, 0, 15f.dpToPx(context))
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        typeCardLabel.setTextColor(DSSColors.textPrimary())
        updatePaymentOptionsUI()
    }

    fun updatePaymentOptionsUI() {
        removeAllViews()
        if (isEditingPaymentMethod) {
            addView(buildOptionRow(PaymentType.CREDIT_CARD, AppStrings.brand(context, "card_plan_credit_card", "Cartão de crédito"), editing = true))
            addView(
                buildOptionRow(PaymentType.PIX, "Pix", editing = true),
                LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 10f.dpToPx(context)
                },
            )
        } else {
            addView(buildOptionRow(selectedPaymentType, labelFor(selectedPaymentType), editing = false))
        }
    }

    private fun labelFor(type: PaymentType): String =
        if (type == PaymentType.PIX) "Pix" else AppStrings.brand(context, "card_plan_credit_card", "Cartão de crédito")

    private fun buildOptionRow(type: PaymentType, name: String, editing: Boolean): android.view.View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val sz = 30f.dpToPx(context)
        val icon = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(iconResolver(type))
        }
        val label = TextView(context).apply {
            text = name
            textSize = 14f
            typeface = DSSFont.light(context, 14f).typeface
            setTextColor(DSSColors.textPrimary())
        }
        val action = TextView(context).apply {
            val title = if (editing) "Selecionar" else "Alterar"
            text = SpannableString(title).apply { setSpan(UnderlineSpan(), 0, length, 0) }
            textSize = 14f
            typeface = DSSFont.light(context, 14f).typeface
            setTextColor(DSSColors.textLink())
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (editing) {
                    selectedPaymentType = type
                    isEditingPaymentMethod = false
                    delegate?.paymentTypeSelected(if (type == PaymentType.CREDIT_CARD) 0 else 1)
                    delegate?.didToggleEditingMode(false)
                } else {
                    isEditingPaymentMethod = true
                    delegate?.didToggleEditingMode(true)
                }
                updatePaymentOptionsUI()
            }
        }
        row.addView(icon, LayoutParams(sz, sz))
        row.addView(
            label,
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                // iOS: opção selecionada (recolhida) usa icon.trailing + 25; nas opções
                // em edição usa + 4 (addSelectedPaymentOption vs updatePaymentOptionsUI).
                leftMargin = (if (editing) 4f else 25f).dpToPx(context)
            },
        )
        row.addView(FrameLayout(context), LayoutParams(0, 1, 1f))
        row.addView(action)
        return row
    }
}
