package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dssText

/**
 * Status curto (Figma do Grupo de Recarga), em dois formatos:
 * - [Style.DOT]: bolinha + texto ("● Programada", "● Sem programada");
 * - [Style.PILL]: texto dentro de uma pílula ("Inativa", "Vinculado", "Ativa").
 * O [Tone] escolhe as cores: [Tone.SUCCESS] verde, [Tone.NEUTRAL] cinza, [Tone.ERROR] vermelho.
 */
class DSSStatusLabel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class Style { DOT, PILL }
    enum class Tone { SUCCESS, NEUTRAL, ERROR }

    private val dot = View(context)
    private val label = dssText(context, DSSWeight.MEDIUM, 12f, DSSColors.textSecondary(), maxLines = 1)

    var style: Style = Style.DOT
        private set
    var tone: Tone = Tone.NEUTRAL
        private set

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val dotSize = 8f.dpToPx(context)
        addView(dot, LayoutParams(dotSize, dotSize).apply { marginEnd = 6f.dpToPx(context) })
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        refresh()
        setupThemeObserver()
    }

    fun configure(text: CharSequence, tone: Tone, style: Style = Style.DOT) {
        label.text = text
        this.tone = tone
        this.style = style
        contentDescription = text
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val strong = toneColor()
        when (style) {
            Style.DOT -> {
                dot.visibility = VISIBLE
                dot.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (tone == Tone.NEUTRAL) DSSColors.textTertiary() else strong)
                }
                label.textSize = 12f
                label.setTextColor(if (tone == Tone.NEUTRAL) DSSColors.textSecondary() else darken(strong))
                background = null
                setPadding(0, 0, 0, 0)
            }
            Style.PILL -> {
                dot.visibility = GONE
                label.textSize = 11f
                label.setTextColor(if (tone == Tone.NEUTRAL) DSSColors.textSecondary() else strong)
                val fill = if (tone == Tone.NEUTRAL) {
                    ColorUtils.setAlphaComponent(DSSColors.textPrimary(), 18)
                } else {
                    ColorUtils.setAlphaComponent(strong, 30)
                }
                background = DrawableFactory.rounded(context, fill, cornerRadiusDp = 9f)
                val h = 8f.dpToPx(context)
                val v = 3f.dpToPx(context)
                setPadding(h, v, h, v)
            }
        }
    }

    @ColorInt
    private fun toneColor(): Int = when (tone) {
        Tone.SUCCESS -> DSSColors.success()
        Tone.ERROR -> DSSColors.error()
        Tone.NEUTRAL -> DSSColors.textSecondary()
    }

    /** Texto do status com ponto: o verde do Figma é mais escuro que o da bolinha. */
    @ColorInt
    private fun darken(@ColorInt color: Int): Int =
        if (ColorUtils.calculateLuminance(DSSColors.background()) > 0.5) {
            ColorUtils.blendARGB(color, DSSColors.textPrimary(), 0.35f)
        } else {
            color
        }
}
