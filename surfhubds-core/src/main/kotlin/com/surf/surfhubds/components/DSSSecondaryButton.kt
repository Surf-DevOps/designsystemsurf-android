package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSSecondaryButton` do iOS — botão com borda e fundo transparente.
 */
class DSSSecondaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatButton(context, attrs, defStyleAttr), ThemeAware {

    var onTap: (() -> Unit)? = null

    @ColorInt var borderColorOverride: Int? = null
        set(value) { field = value; refresh() }

    var borderWidthDp: Float = 2f
        set(value) { field = value; refresh() }

    var cornerRadiusDp: Float = 25f
        set(value) { field = value; refresh() }

    init {
        gravity = Gravity.CENTER
        isAllCaps = false
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        minHeight = 50f.dpToPx(context)
        setOnClickListener { onTap?.invoke() }
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackground(DrawableFactory.rounded(
            context = context,
            backgroundColor = android.graphics.Color.TRANSPARENT,
            cornerRadiusDp = cornerRadiusDp,
            strokeColor = borderColorOverride ?: DSSColors.primary(),
            strokeWidthDp = borderWidthDp,
        ))
        setTextColor(DSSColors.primary())
    }
}
