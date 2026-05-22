package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSActionCardButton` do iOS — botão "action card" com tamanho fixo,
 * canto arredondado, borda e título alinhado ao bottom-left.
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
        textSize = 15f
        typeface = DSSFont.regular(context, 15f).typeface
        // padding: top 18, leading 14, bottom 18, trailing 14
        val padH = 14f.dpToPx(context)
        val padV = 18f.dpToPx(context)
        setPadding(padH, padV, padH, padV)
        minWidth = DEFAULT_WIDTH_DP.dpToPx(context)
        minHeight = DEFAULT_HEIGHT_DP.dpToPx(context)
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = 12f,
            strokeColor = DSSColors.borderDefault(),
            strokeWidthDp = 1f,
        )
        setTextColor(DSSColors.textPrimary())
    }
}
