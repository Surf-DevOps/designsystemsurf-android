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
 * Port do `DSSPixInfoCardView` do iOS — card com 3 colunas (Número, Plano, Valor),
 * cada uma com um label de header e o respectivo valor abaixo.
 */
class DSSPixInfoCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }
    private val dataRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    private val numberHeaderLabel = headerText("Número", Gravity.START)
    private val planHeaderLabel = headerText("Plano", Gravity.CENTER)
    private val valueHeaderLabel = headerText("Valor", Gravity.END)

    private val numberLabel = dataText(Gravity.START)
    private val planLabel = dataText(Gravity.CENTER)
    private val valueLabel = dataText(Gravity.END)

    var cornerRadiusDp: Float = 12f
        set(value) { field = value; refresh() }

    /**
     * Espelha o `backgroundColor` público do iOS (default `systemGray6` ->
     * [DSSColors.backgroundSecondary]). Quando `null`, usa o token semântico.
     */
    @ColorInt
    var backgroundColorOverride: Int? = null
        set(value) { field = value; refresh() }

    init {
        addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        val hPad = 20f.dpToPx(context)
        val vPad = 12f.dpToPx(context)
        container.setPadding(hPad, vPad, hPad, vPad)

        // Header row — pesos: 40 / 25 / 35, spacing 8dp entre colunas (iOS stackView.spacing = 8)
        val columnSpacing = 8f.dpToPx(context)
        headerRow.addView(numberHeaderLabel, weighted(0.40f))
        headerRow.addView(planHeaderLabel, weighted(0.25f, leftMargin = columnSpacing))
        headerRow.addView(valueHeaderLabel, weighted(0.35f, leftMargin = columnSpacing))

        // Data row — mesmos pesos
        dataRow.addView(numberLabel, weighted(0.40f))
        dataRow.addView(planLabel, weighted(0.25f, leftMargin = columnSpacing))
        dataRow.addView(valueLabel, weighted(0.35f, leftMargin = columnSpacing))

        container.addView(
            headerRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        container.addView(
            dataRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 6f.dpToPx(context) },
        )

        refresh()
        setupThemeObserver()
    }

    /** Configura com `msisdn`, plano e preço (centavos -> R$). */
    fun configure(msisdn: String? = null, plan: String, priceInCents: Int) {
        numberLabel.text = msisdn?.let { formatPhoneNumber(it) } ?: "-"
        planLabel.text = plan

        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        valueLabel.text = formatter.format(priceInCents / 100.0)
    }

    /** Configura com valores já formatados. */
    fun configure(number: String? = null, plan: String, value: String) {
        numberLabel.text = number ?: "-"
        planLabel.text = plan
        valueLabel.text = value
    }

    /** Custom colors para os campos plan/value. */
    fun setTextColors(@ColorInt planColor: Int? = null, @ColorInt valueColor: Int? = null) {
        planColor?.let { planLabel.setTextColor(it) }
        valueColor?.let { valueLabel.setTextColor(it) }
    }

    /** Custom typefaces. */
    fun setFonts(planTypeface: Typeface? = null, valueTypeface: Typeface? = null) {
        planTypeface?.let { planLabel.typeface = it }
        valueTypeface?.let { valueLabel.typeface = it }
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = backgroundColorOverride ?: DSSColors.backgroundSecondary(),
            cornerRadiusDp = cornerRadiusDp,
        )

        listOf(numberHeaderLabel, planHeaderLabel, valueHeaderLabel).forEach {
            it.setTextColor(DSSColors.textSecondary())
        }
        listOf(numberLabel, planLabel, valueLabel).forEach {
            it.setTextColor(DSSColors.textPrimary())
        }
    }

    private fun headerText(value: String, gravityValue: Int) = TextView(context).apply {
        text = value
        textSize = 12f
        typeface = DSSFont.regular(context, 12f).typeface
        gravity = gravityValue or Gravity.CENTER_VERTICAL
    }

    private fun dataText(gravityValue: Int) = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.medium(context, 14f).typeface
        gravity = gravityValue or Gravity.CENTER_VERTICAL
    }

    private fun weighted(weight: Float, leftMargin: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight,
        ).apply { this.leftMargin = leftMargin }

    private fun formatPhoneNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.length >= 11) {
            val ddd = digits.substring(0, 2)
            val first = digits.substring(2, 3)
            val middle = digits.substring(3, 7)
            val last = digits.substring(7, 11)
            return "($ddd) $first $middle-$last"
        }
        return raw
    }

    companion object {
        @JvmStatic
        fun defaultStyle(context: Context): DSSPixInfoCardView {
            return DSSPixInfoCardView(context).apply {
                cornerRadiusDp = 12f
                backgroundColorOverride = DSSColors.backgroundSecondary()
            }
        }
    }
}
