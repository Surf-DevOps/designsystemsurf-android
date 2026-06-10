package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import java.text.NumberFormat
import java.util.Locale

/**
 * Port do `DSSResumeCard.swift` (iOS) — card de resumo com 3 colunas (Número, Oferta, Valor).
 */
class DSSResumeCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    private val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val titleLabel = TextView(context).apply {
        text = "Resumo"
        textSize = 18f
        typeface = DSSFont.medium(context, 18f).typeface
        gravity = Gravity.START
    }
    private val numberLabel = TextView(context).apply {
        text = "Número"; textSize = 14f; typeface = DSSFont.light(context, 14f).typeface
    }
    private val numberValue = TextView(context).apply { textSize = 11f; typeface = DSSFont.medium(context, 11f).typeface }
    private val offerLabel = TextView(context).apply {
        text = "Oferta"; textSize = 14f; typeface = DSSFont.light(context, 14f).typeface
    }
    private val offerValue = TextView(context).apply { textSize = 11f; typeface = DSSFont.medium(context, 11f).typeface }
    private val priceLabel = TextView(context).apply {
        text = "Valor"; textSize = 14f; typeface = DSSFont.light(context, 14f).typeface
    }
    private val priceValue = TextView(context).apply { textSize = 11f; typeface = DSSFont.medium(context, 11f).typeface }

    var cornerRadiusDp: Float = 12f
        set(value) { field = value; refresh() }

    var borderWidthDp: Float = 1f
        set(value) { field = value; refresh() }

    @ColorInt
    var borderColorOverride: Int? = null
        set(value) { field = value; refresh() }

    @ColorInt
    var backgroundColorOverride: Int? = null
        set(value) { field = value; refresh() }

    var titleFont: Typeface? = null
        set(value) { field = value; if (value != null) titleLabel.typeface = value }

    @ColorInt
    var titleTextColorOverride: Int? = null
        set(value) { field = value; if (value != null) titleLabel.setTextColor(value) }

    init {
        val pad = 16f.dpToPx(context)
        container.setPadding(pad, pad, pad, pad)
        container.addView(titleLabel)

        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val col1 = makeColumn(numberLabel, numberValue)
        val col2 = makeColumn(offerLabel, offerValue)
        val col3 = makeColumn(priceLabel, priceValue)
        // iOS: numberLabel/offerLabel width 100, gap 20 entre colunas; priceLabel ocupa o restante.
        val gap = 20f.dpToPx(context)
        val colWidth = 100f.dpToPx(context)
        row.addView(
            col1,
            LinearLayout.LayoutParams(colWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginEnd = gap },
        )
        row.addView(
            col2,
            LinearLayout.LayoutParams(colWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginEnd = gap },
        )
        row.addView(
            col3,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )

        container.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 20f.dpToPx(context) },
        )
        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        refresh()
        setupThemeObserver()
    }

    private fun makeColumn(label: TextView, value: TextView): LinearLayout {
        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        col.addView(label)
        col.addView(
            value,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 4f.dpToPx(context) },
        )
        return col
    }

    /** Configura o card com número, oferta e preço (centavos). */
    fun configure(title: String? = null, number: String, offer: String, priceInCents: Int) {
        if (title != null) titleLabel.text = title
        numberValue.text = formatPhone(number)
        offerValue.text = offer
        priceValue.text = formatPrice(priceInCents)
    }

    fun setNumber(number: String) { numberValue.text = formatPhone(number) }
    fun setOffer(offer: String) { offerValue.text = offer }
    fun setPrice(cents: Int) { priceValue.text = formatPrice(cents) }
    fun setTitle(title: String) { titleLabel.text = title }

    /** Configura a cor do título (iOS: setTitleColor). */
    fun setTitleColor(@ColorInt color: Int) { titleTextColorOverride = color }

    /** Configura o título com fonte e cor (iOS: setTitle(_:font:color:)). */
    fun setTitle(title: String, font: Typeface, @ColorInt color: Int? = null) {
        titleLabel.text = title
        titleFont = font
        if (color != null) titleTextColorOverride = color
    }

    fun setCategoryLabels(number: String = "Número", offer: String = "Oferta", price: String = "Valor") {
        numberLabel.text = number; offerLabel.text = offer; priceLabel.text = price
    }

    private fun formatPhone(number: String): String {
        val clean = number.filter { it.isDigit() }
        if (clean.length < 10) return number
        var phone = clean
        if (clean.length == 13 && clean.startsWith("55")) phone = clean.drop(2)
        if (phone.length == 11) {
            val area = phone.substring(0, 2)
            val first = phone.substring(2, 7)
            val second = phone.substring(7)
            return "($area) $first-$second"
        }
        return number
    }

    private fun formatPrice(cents: Int): String {
        val value = cents / 100.0
        return try {
            val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            nf.minimumFractionDigits = 2
            nf.maximumFractionDigits = 2
            nf.format(value)
        } catch (_: Throwable) {
            String.format(Locale.US, "R$ %d,%02d", cents / 100, cents % 100)
        }
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        container.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = backgroundColorOverride ?: DSSColors.surface(),
            cornerRadiusDp = cornerRadiusDp,
            strokeColor = borderColorOverride ?: DSSColors.borderDefault(),
            strokeWidthDp = borderWidthDp,
        )
        titleLabel.setTextColor(titleTextColorOverride ?: DSSColors.textPrimary())
        val secondary = DSSColors.textSecondary()
        val primary = DSSColors.textPrimary()
        numberLabel.setTextColor(secondary)
        offerLabel.setTextColor(secondary)
        priceLabel.setTextColor(secondary)
        numberValue.setTextColor(primary)
        offerValue.setTextColor(primary)
        priceValue.setTextColor(primary)
    }

    companion object {
        /** Card de resumo com estilo padrão. */
        fun defaultStyle(context: Context): DSSResumeCard = DSSResumeCard(context).apply {
            cornerRadiusDp = 12f
            borderWidthDp = 1f
        }

        /** Card de resumo destacado (borda primary, 2dp). */
        fun prominentStyle(context: Context): DSSResumeCard = DSSResumeCard(context).apply {
            cornerRadiusDp = 16f
            borderWidthDp = 2f
            borderColorOverride = DSSColors.primary()
        }

        /** Card sem borda. */
        fun borderlessStyle(context: Context): DSSResumeCard = DSSResumeCard(context).apply {
            cornerRadiusDp = 12f
            borderWidthDp = 0f
        }
    }
}
