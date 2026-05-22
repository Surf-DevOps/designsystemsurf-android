package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSPrincipalButton` do iOS — botão principal com fill da brand.
 */
class DSSPrincipalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatButton(context, attrs, defStyleAttr), ThemeAware {

    var onTap: (() -> Unit)? = null

    var cornerRadiusDp: Float = 25f
        set(value) { field = value; refresh() }

    init {
        gravity = Gravity.CENTER
        isAllCaps = false
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        minHeight = 50.dpToPx(context)
        setOnClickListener { onTap?.invoke() }
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackground(DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.primary(),
            cornerRadiusDp = cornerRadiusDp,
        ))
        setTextColor(DSSColors.buttonText())
    }
}
