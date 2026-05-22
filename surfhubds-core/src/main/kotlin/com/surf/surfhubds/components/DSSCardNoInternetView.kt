package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSCardNoInternetView` do iOS — cartão fundo escuro "Não fique sem internet!"
 * com porcentagem consumida, validade, escolha pix/crédito e swipe para antecipar.
 */
class DSSCardNoInternetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class AppearanceStyle { DARK, LIGHT }

    interface Delegate {
        fun didToggleEditingMode(isEditing: Boolean)
        fun paymentTypeSelected(type: PaymentType)
        fun didCompleteAnticipateSlide(type: PaymentType)
    }

    var delegate: Delegate? = null

    /** Resolver dos ícones (Master / Pix). */
    var iconResolver: (PaymentType) -> android.graphics.drawable.Drawable? = { null }

    private var currentPaymentType: PaymentType = PaymentType.PIX
    private var isEditingPaymentMethod = false
    private var appearanceStyle: AppearanceStyle = AppearanceStyle.DARK

    // background card
    private val backgroundCard = FrameLayout(context)

    // título
    private val titleLabel = TextView(context).apply {
        text = "Não fique sem internet!"
        textSize = 17f
        typeface = DSSFont.bold(context, 17f).typeface
        setTextColor(Color.WHITE)
    }
    private val dividerLine = android.view.View(context)
    private val percentageContainer = FrameLayout(context).apply {
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = Color.rgb(230, 51, 51),
            cornerRadiusDp = 10f,
        )
    }
    private val percentageLabel = TextView(context).apply {
        text = "80% consumida"
        textSize = 12f
        typeface = DSSFont.medium(context, 12f).typeface
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
    }
    private val validityLabel = TextView(context).apply {
        text = "Vencimento: 20/12/23"
        textSize = 11f
        typeface = DSSFont.regular(context, 11f).typeface
        setTextColor(Color.argb(178, 255, 255, 255))
        gravity = Gravity.END
    }

    private val paymentOptionsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val anticipateSwipeView = DSSSwipeView(context).apply {
        textColor = Color.WHITE
        sliderCornerRadiusDp = 22f
        thumbnailTintColor = Color.WHITE
        sliderBackgroundColor = Color.rgb(204, 0, 0)
        textLabel.text = "  Antecipar recarga   R$ 50,00"
    }

    init {
        setupTree()
        applyAppearance()
        updatePaymentOptionsUI()
        refresh()
        setupThemeObserver()
        anticipateSwipeView.onCompleted = {
            delegate?.didCompleteAnticipateSlide(currentPaymentType)
        }
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        applyAppearance()
    }

    private fun setupTree() {
        // background card preenche o componente
        addView(
            backgroundCard,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )

        val pad20 = 20f.dpToPx(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad20, pad20, pad20, pad20)
        }

        container.addView(titleLabel)
        container.addView(
            dividerLine,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1f.dpToPx(context)).apply {
                topMargin = 5f.dpToPx(context)
            },
        )

        // Row percentage + validity
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val percLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            24f.dpToPx(context),
        )
        val pad10 = 10f.dpToPx(context)
        percentageContainer.setPadding(pad10, 0, pad10, 0)
        percentageContainer.addView(
            percentageLabel,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            },
        )
        row.addView(percentageContainer, percLp)
        val spacer = android.view.View(context)
        row.addView(
            spacer,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
        )
        row.addView(validityLabel)
        container.addView(
            row,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 12f.dpToPx(context)
            },
        )

        container.addView(
            paymentOptionsContainer,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 16f.dpToPx(context)
            },
        )

        container.addView(
            anticipateSwipeView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 44f.dpToPx(context)).apply {
                topMargin = 12f.dpToPx(context)
            },
        )

        backgroundCard.addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
    }

    fun setAppearanceStyle(style: AppearanceStyle) {
        appearanceStyle = style
        applyAppearance()
    }

    private fun applyAppearance() {
        when (appearanceStyle) {
            AppearanceStyle.DARK -> {
                backgroundCard.background = DrawableFactory.rounded(
                    context = context,
                    backgroundColor = Color.BLACK,
                    cornerRadiusDp = 12f,
                    strokeColor = Color.WHITE,
                    strokeWidthDp = 1f,
                )
                titleLabel.setTextColor(Color.WHITE)
                dividerLine.setBackgroundColor(Color.rgb(77, 77, 77))
                validityLabel.setTextColor(Color.argb(178, 255, 255, 255))
            }
            AppearanceStyle.LIGHT -> {
                backgroundCard.background = DrawableFactory.rounded(
                    context = context,
                    backgroundColor = DSSColors.backgroundSecondary(),
                    cornerRadiusDp = 12f,
                )
                titleLabel.setTextColor(DSSColors.textPrimary())
                dividerLine.setBackgroundColor(DSSColors.divider())
                validityLabel.setTextColor(DSSColors.textSecondary())
            }
        }
    }

    private fun updatePaymentOptionsUI() {
        paymentOptionsContainer.removeAllViews()
        if (isEditingPaymentMethod) {
            paymentOptionsContainer.addView(
                buildExpandedRow(PaymentType.CREDIT_CARD, "Cartão de crédito"),
            )
            paymentOptionsContainer.addView(
                buildExpandedRow(PaymentType.PIX, "Pix"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 12f.dpToPx(context)
                },
            )
        } else {
            paymentOptionsContainer.addView(buildCollapsedRow())
        }
    }

    private fun buildCollapsedRow(): android.view.View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(iconResolver(currentPaymentType))
        }
        val label = TextView(context).apply {
            text = if (currentPaymentType == PaymentType.PIX) "Pix" else "Cartão de crédito"
            textSize = 15f
            typeface = DSSFont.regular(context, 15f).typeface
            setTextColor(Color.WHITE)
        }
        val alterButton = TextView(context).apply {
            text = SpannableString("Alterar").apply { setSpan(UnderlineSpan(), 0, length, 0) }
            textSize = 13f
            typeface = DSSFont.regular(context, 13f).typeface
            setTextColor(Color.WHITE)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                isEditingPaymentMethod = true
                updatePaymentOptionsUI()
                delegate?.didToggleEditingMode(true)
            }
        }
        val sz = 28f.dpToPx(context)
        row.addView(icon, LinearLayout.LayoutParams(sz, sz))
        row.addView(
            label,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = 12f.dpToPx(context)
            },
        )
        row.addView(
            android.view.View(context),
            LinearLayout.LayoutParams(0, 1, 1f),
        )
        row.addView(alterButton)
        return row
    }

    private fun buildExpandedRow(type: PaymentType, name: String): android.view.View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(iconResolver(type))
        }
        val label = TextView(context).apply {
            text = name
            textSize = 14f
            typeface = DSSFont.regular(context, 14f).typeface
            setTextColor(Color.WHITE)
        }
        val selectButton = TextView(context).apply {
            text = SpannableString("Selecionar").apply { setSpan(UnderlineSpan(), 0, length, 0) }
            textSize = 13f
            typeface = DSSFont.regular(context, 13f).typeface
            setTextColor(Color.WHITE)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                currentPaymentType = type
                isEditingPaymentMethod = false
                updatePaymentOptionsUI()
                delegate?.didToggleEditingMode(false)
                delegate?.paymentTypeSelected(type)
            }
        }
        val sz = 28f.dpToPx(context)
        row.addView(icon, LinearLayout.LayoutParams(sz, sz))
        row.addView(
            label,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = 12f.dpToPx(context)
            },
        )
        row.addView(
            android.view.View(context),
            LinearLayout.LayoutParams(0, 1, 1f),
        )
        row.addView(selectButton)
        return row
    }

    fun configure(
        consumedPercentage: Int,
        expiryDate: String,
        paymentType: PaymentType = PaymentType.PIX,
        anticipateAmount: Double = 50.00,
    ) {
        percentageLabel.text = "${consumedPercentage}% consumida"
        validityLabel.text = "Vencimento: $expiryDate"
        currentPaymentType = paymentType
        updatePaymentOptionsUI()
        val formatted = String.format("R$ %.2f", anticipateAmount).replace('.', ',')
        anticipateSwipeView.textLabel.text = "  Antecipar recarga   $formatted"
    }

    fun resetSlider() {
        anticipateSwipeView.resetState(animated = true)
    }
}
