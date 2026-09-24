package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.ColorUtils
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver

/**
 * Ícone dentro de um círculo, com as cores vindas dos tokens:
 * - [Style.SUCCESS]: círculo `success` cheio e ícone `textOnPrimary` (telas de sucesso);
 * - [Style.ERROR]: círculo `error` a 10% e ícone `error` (avisos de erro);
 * - [Style.PRIMARY_SOFT]: círculo `primary` a 8% e ícone `primary` (ícones de apoio);
 * - [Style.PRIMARY]: círculo `primaryButton` cheio e ícone `textOnPrimary` (atalhos).
 * Por padrão o ícone ocupa ~50% do círculo; com [autoPadding] = false vale o padding
 * definido por quem usa. O tamanho vem do layout.
 */
class DSSIconBadge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr), ThemeAware {

    enum class Style { SUCCESS, ERROR, PRIMARY_SOFT, PRIMARY }

    var style: Style = Style.PRIMARY_SOFT
        set(value) { field = value; refresh() }

    var autoPadding: Boolean = true

    init {
        scaleType = ScaleType.FIT_CENTER
        refresh()
        setupThemeObserver()
    }

    fun configure(icon: Drawable?, style: Style) {
        setImageDrawable(icon)
        this.style = style
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!autoPadding) return
        val pad = minOf(w, h) / 4
        setPadding(pad, pad, pad, pad)
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val (fill, tint) = when (style) {
            Style.SUCCESS -> DSSColors.success() to DSSColors.textOnPrimary()
            Style.ERROR -> ColorUtils.setAlphaComponent(DSSColors.error(), SOFT_ALPHA) to DSSColors.error()
            Style.PRIMARY -> DSSColors.primaryButton() to DSSColors.textOnPrimary()
            Style.PRIMARY_SOFT -> ColorUtils.setAlphaComponent(DSSColors.primary(), PRIMARY_SOFT_ALPHA) to DSSColors.primary()
        }
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
        }
        setColorFilter(tint)
    }

    private companion object {
        const val SOFT_ALPHA = 26 // ~10%
        const val PRIMARY_SOFT_ALPHA = 20 // ~8%: primary é mais saturado que o error
    }
}
