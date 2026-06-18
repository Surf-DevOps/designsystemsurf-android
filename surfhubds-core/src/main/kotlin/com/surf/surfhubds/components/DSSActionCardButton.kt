package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSActionCardButton` do iOS — botão "action card" com tamanho fixo,
 * canto arredondado, borda, sombra e título alinhado ao bottom-left.
 *
 * Espelha o `applyColors()` do iOS: o card NÃO usa tokens semânticos de brand,
 * e sim cores fixas que variam apenas por `ColorScheme` (light/dark/black),
 * exatamente como o original (`.white` / `.secondarySystemBackground` / `.black`).
 */
class DSSActionCardButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatButton(context, attrs, defStyleAttr), ThemeAware {

    companion object {
        const val DEFAULT_WIDTH_DP: Float = 140f
        const val DEFAULT_HEIGHT_DP: Float = 105f
    }

    /** `convenience init(title:)` do iOS. */
    constructor(context: Context, title: CharSequence) : this(context) {
        cardTitle = title
    }

    var cardTitle: CharSequence = ""
        set(value) {
            field = value
            text = value
        }

    init {
        isAllCaps = false
        gravity = Gravity.BOTTOM or Gravity.START
        textAlignment = TEXT_ALIGNMENT_TEXT_START
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        textSize = 15f
        typeface = DSSFont.regular(context, 15f).typeface
        // contentInsets iOS: top 18, leading 14, bottom 18, trailing 14
        val padH = 14f.dpToPx(context)
        val padV = 18f.dpToPx(context)
        setPadding(padH, padV, padH, padV)
        minWidth = DEFAULT_WIDTH_DP.dpToPx(context)
        minHeight = DEFAULT_HEIGHT_DP.dpToPx(context)
        // Remove o stateListAnimator (AppCompatButton aplica elevação/sombra default que muda
        // ao pressionar). Aplicamos uma sombra suave fixa que aproxima o iOS:
        // shadow black@8% radius 6 offset (0,2). No Android a elevation gera a sombra;
        // ~4dp + cor de sombra translúcida dá um resultado próximo do "soft shadow".
        stateListAnimator = null
        elevation = 4f.dpToPx(context).toFloat()
        outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        // Tinta da sombra (API 28+): black@~8% pra aproximar o shadowColor/opacity do iOS.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val softShadow = androidx.core.graphics.ColorUtils.setAlphaComponent(Color.BLACK, 20) // ~8%
            outlineAmbientShadowColor = softShadow
            outlineSpotShadowColor = softShadow
        }
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val scheme = ThemeManager.colorScheme
        val isBlack = scheme == ColorScheme.BLACK
        val isDark = scheme == ColorScheme.DARK || isBlack

        // backgroundColor = isBlack ? .black : (isDark ? .secondarySystemBackground : .white)
        val bg = when {
            isBlack -> Color.BLACK
            isDark -> Color.rgb(28, 28, 30) // secondarySystemBackground (dark)
            else -> Color.WHITE
        }
        // borderColor = isDark ? systemGray4 : UIColor(white: 0.85)
        val border = if (isDark) {
            Color.rgb(58, 58, 60) // systemGray4 (dark)
        } else {
            Color.rgb(217, 217, 217) // UIColor(white: 0.85)
        }

        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = bg,
            cornerRadiusDp = 12f,
            strokeColor = border,
            strokeWidthDp = 1f,
        )
        // baseForegroundColor = isDark ? .white : .black
        setTextColor(if (isDark) Color.WHITE else Color.BLACK)
    }
}
