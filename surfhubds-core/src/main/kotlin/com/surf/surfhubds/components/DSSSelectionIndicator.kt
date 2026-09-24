package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.surf.surfhubds.R
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Indicador de seleção em círculo (Figma): marcado = círculo `primary` com check
 * `textOnPrimary`; desmarcado = só o contorno. Não trata clique — quem usa controla
 * [isChecked] (ex.: card de pacote, linha a creditar).
 */
class DSSSelectionIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr), ThemeAware {

    var isChecked: Boolean = false
        set(value) { field = value; refresh() }

    init {
        scaleType = ScaleType.CENTER_INSIDE
        val pad = 4f.dpToPx(context)
        setPadding(pad, pad, pad, pad)
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (isChecked) {
                setColor(DSSColors.primary())
            } else {
                setColor(Color.TRANSPARENT)
                setStroke(1.5f.dpToPx(context), DSSColors.borderSubtle())
            }
        }
        if (isChecked) {
            setImageResource(R.drawable.dss_ic_check)
            setColorFilter(DSSColors.textOnPrimary())
        } else {
            setImageDrawable(null)
        }
    }
}
