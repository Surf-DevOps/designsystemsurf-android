package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSPaymentMethodCard` do iOS — card de método de pagamento
 * (Pix, novo cartão ou cartão existente).
 *
 * Estilo: 56dp de altura, canto de 12dp, borda quando selecionado.
 */
class DSSPaymentMethodCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    private val iconImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    private val titleLabel = TextView(context).apply {
        textSize = 16f
        typeface = DSSFont.medium(context, 16f).typeface
        maxLines = 1
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
    }
    private val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    /** Selected state — manipulado via [setSelected]. */
    private var _selected: Boolean = false
    val isCardSelected: Boolean get() = _selected

    var cornerRadiusDp: Float = 12f
        set(value) { field = value; refresh() }

    @ColorInt
    var cardBorderColor: Int = Color.TRANSPARENT
        set(value) { field = value; refresh() }

    var cardBorderWidthDp: Float = 0f
        set(value) { field = value; refresh() }

    init {
        val pad = 16f.dpToPx(context)
        val iconSize = 24f.dpToPx(context)

        row.addView(
            iconImageView,
            LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = 12f.dpToPx(context)
            },
        )
        row.addView(
            titleLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )

        addView(
            row,
            LayoutParams(LayoutParams.MATCH_PARENT, 56f.dpToPx(context)).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = pad
                rightMargin = pad
            },
        )

        refresh()
        setupThemeObserver()
    }

    /**
     * Construtor de conveniência alinhado ao iOS.
     */
    constructor(context: Context, icon: Drawable?, title: String) : this(context) {
        configure(icon, title)
    }

    fun configure(icon: Drawable?, title: String) {
        iconImageView.setImageDrawable(icon)
        titleLabel.text = title
        applyIconTint()
    }

    fun setIcon(icon: Drawable?) {
        iconImageView.setImageDrawable(icon)
        applyIconTint()
    }
    fun setTitleText(title: String) { titleLabel.text = title }

    /** Atualiza visual selecionado / não selecionado. */
    fun setSelectedState(selected: Boolean) {
        _selected = selected
        refresh()
    }

    fun toggleSelection() = setSelectedState(!_selected)

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        // iOS: colorScheme == .black ? DSSColors.primaryButton : DSSColors.primary
        val accent = if (ThemeManager.colorScheme == ColorScheme.BLACK) {
            DSSColors.primaryButton()
        } else {
            DSSColors.primary()
        }
        // iOS: containerView.backgroundColor = .systemBackground (branco no light, preto
        // no dark) — não o token `surface`.
        val isDark = ThemeManager.colorScheme == ColorScheme.DARK ||
            ThemeManager.colorScheme == ColorScheme.BLACK
        val systemBackground = if (isDark) Color.BLACK else Color.WHITE
        if (_selected) {
            background = DrawableFactory.rounded(
                context = context,
                backgroundColor = withAlpha(accent, 0x0D), // ~5%
                cornerRadiusDp = cornerRadiusDp,
                strokeColor = accent,
                strokeWidthDp = 2f,
            )
        } else {
            background = DrawableFactory.rounded(
                context = context,
                backgroundColor = systemBackground,
                cornerRadiusDp = cornerRadiusDp,
                strokeColor = if (cardBorderWidthDp > 0f) cardBorderColor else null,
                strokeWidthDp = cardBorderWidthDp,
            )
        }
        titleLabel.setTextColor(DSSColors.textPrimary())
        applyIconTint()
    }

    /**
     * Espelha o `tintColor` do iOS: só imagens template (SF Symbols → vetores no Android)
     * recebem tint; assets raster da brand (ex.: ícone do Pix em teal, bandeiras dos
     * cartões) são exibidos com as cores originais (rendering `.automatic` no iOS).
     */
    private fun applyIconTint() {
        if (iconImageView.drawable is BitmapDrawable) {
            iconImageView.clearColorFilter()
        } else {
            iconImageView.setColorFilter(DSSColors.textPrimary(), PorterDuff.Mode.SRC_IN)
        }
    }

    private fun withAlpha(@ColorInt color: Int, alpha: Int): Int {
        val a = alpha and 0xFF
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    /**
     * Fábricas de conveniência espelhando a extensão Swift `DSSPaymentMethodCard`.
     * No iOS as funções não recebem contexto (UIKit); aqui o [Context] é obrigatório
     * para resolver ícones/recursos.
     */
    companion object {

        /** Cria um card Pix. */
        @JvmStatic
        fun pixCard(context: Context): DSSPaymentMethodCard =
            DSSPaymentMethodCard(context, PaymentMethodImages.pixIcon(context), "Pix")

        /** Cria um card para novo cartão. */
        @JvmStatic
        fun newCardCard(context: Context): DSSPaymentMethodCard =
            DSSPaymentMethodCard(
                context,
                PaymentMethodImages.addCardIcon(context),
                "Novo Cartão de Crédito",
            )

        /** Cria um card para cartão existente. */
        @JvmStatic
        fun creditCard(
            context: Context,
            lastFourDigits: String,
            cardBrand: String = "",
        ): DSSPaymentMethodCard {
            val cardIcon = PaymentMethodImages.cardBrandIcon(context, cardBrand)
            val title = if (cardBrand.isEmpty()) {
                "**** $lastFourDigits"
            } else {
                "$cardBrand **** $lastFourDigits"
            }
            return DSSPaymentMethodCard(context, cardIcon, title)
        }
    }
}
