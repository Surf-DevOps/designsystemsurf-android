package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
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
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import kotlin.math.roundToInt

/**
 * Port do `DSSConsumptionCard` do iOS — cartão horizontal "Total disponível" exibindo
 * ícone + título + "usado / total" (Internet, Ligações, SMS).
 */
class DSSConsumptionCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Mesma enumeração / strings do iOS. */
    object CardKind {
        const val INTERNET = "Internet"
        const val CALLS = "Ligações"
        const val SMS = "SMS"
    }

    data class Configuration(
        val cardType: String,
        val usedValue: Int,
        val totalValue: Int,
    )

    /** Resolver de ícone — por padrão sem ícone (no iOS usa SF Symbols). */
    var iconResolver: (cardType: String) -> android.graphics.drawable.Drawable? = { null }

    private val container = FrameLayout(context)
    private val iconView = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    private val titleView = TextView(context).apply {
        text = "Total disponível"
        textSize = 14f
        typeface = DSSFont.medium(context, 14f).typeface
        setTextColor(Color.WHITE)
    }
    private val usedValueView = TextView(context).apply {
        textSize = 18f
        typeface = DSSFont.medium(context, 18f).typeface
        setTextColor(Color.WHITE)
        gravity = Gravity.END
    }
    private val totalValueView = TextView(context).apply {
        textSize = 12f
        typeface = DSSFont.light(context, 12f).typeface
        setTextColor(Color.argb(178, 255, 255, 255))
        gravity = Gravity.END
    }

    init {
        setupTree()
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun setupTree() {
        // Container ocupando 65dp de altura
        addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, 65f.dpToPx(context)),
        )

        // ícone à esquerda + título
        val leftStack = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        leftStack.addView(iconView, LinearLayout.LayoutParams(16f.dpToPx(context), 16f.dpToPx(context)))
        leftStack.addView(
            titleView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 12f.dpToPx(context) },
        )
        val leftLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        leftLp.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        leftLp.leftMargin = 20f.dpToPx(context)
        container.addView(leftStack, leftLp)

        // direita: used / total
        val rightStack = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        rightStack.addView(usedValueView)
        rightStack.addView(
            totalValueView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 5f.dpToPx(context) },
        )
        val rightLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        rightLp.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        rightLp.rightMargin = 20f.dpToPx(context)
        container.addView(rightStack, rightLp)
    }

    private fun refresh() {
        container.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.primary(),
            cornerRadiusDp = 16f,
        )
    }

    fun configure(config: Configuration) {
        totalValueView.visibility = VISIBLE
        when (config.cardType) {
            CardKind.INTERNET -> {
                usedValueView.text = "${formatMbToGb(config.usedValue)}GB"
                totalValueView.text = "/ ${formatMbToGb(config.totalValue)}GB"
            }
            CardKind.CALLS -> {
                if (config.totalValue >= 900) {
                    usedValueView.text = "Ilimitado"
                    totalValueView.visibility = GONE
                } else {
                    usedValueView.text = "${config.usedValue}Min"
                    totalValueView.text = "/ ${config.totalValue}Min"
                }
            }
            CardKind.SMS -> {
                usedValueView.text = "${config.usedValue}SMS"
                totalValueView.text = "/ ${config.totalValue}SMS"
            }
            else -> Unit
        }
        iconView.setImageDrawable(iconResolver(config.cardType))
    }

    private fun formatMbToGb(mb: Int): String {
        val gb = mb / 1000.0
        val rounded = (gb * 10).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}
