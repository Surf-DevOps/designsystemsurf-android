package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory

/**
 * Card de conteúdo neutro (Figma do Compre e Ganhe): fundo `backgroundSecondary`, borda
 * `divider` de 1dp e cantos de 8dp. É um LinearLayout vertical para receber filhos no XML;
 * o padding fica a cargo de quem usa.
 */
class DSSSurfaceCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    init {
        orientation = VERTICAL
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.backgroundSecondary(),
            cornerRadiusDp = 8f,
            strokeColor = DSSColors.divider(),
            strokeWidthDp = 1f,
        )
    }
}
