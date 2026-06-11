package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSSelectionButton` do iOS — botão pill com estado de selected/unselected.
 *
 * Usado por [DSSSelectionButtonView] para construir grids de FAQ.
 */
class DSSSelectionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatButton(context, attrs, defStyleAttr), ThemeAware {

    @ColorInt private var selectedTitleColor: Int = DSSColors.textSecondary()
    @ColorInt private var unselectedTitleColor: Int = Color.DKGRAY
    @ColorInt private var borderColorOverride: Int = DSSColors.primary()

    var onSelectedChange: ((Boolean) -> Unit)? = null

    init {
        isAllCaps = false
        gravity = Gravity.CENTER
        textSize = 15f
        typeface = DSSFont.light(context, 15f).typeface
        minWidth = 129f.dpToPx(context)
        minHeight = 40f.dpToPx(context)
        setOnClickListener {
            isSelected = !isSelected
            onSelectedChange?.invoke(isSelected)
        }
        // Espelha `setupButton` do iOS (aparência inicial, antes de qualquer toggle).
        setupAppearance()
        setupThemeObserver()
    }

    fun configure(
        title: String,
        @ColorInt selectedTitleColor: Int = DSSColors.textSecondary(),
        @ColorInt unselectedTitleColor: Int = Color.DKGRAY,
        @ColorInt borderColor: Int = DSSColors.primary(),
    ) {
        text = title
        this.selectedTitleColor = selectedTitleColor
        this.unselectedTitleColor = unselectedTitleColor
        this.borderColorOverride = borderColor
        setupAppearance()
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        // Espelha o `didSet` de `isSelected` no iOS -> `updateAppearance()`.
        updateAppearance()
    }

    override fun applyTheme(theme: Theme) {
        // Reaplicar a aparência preservando o estado atual de seleção.
        if (isSelected) updateAppearance() else setupAppearance()
    }

    /**
     * Espelha `setupButton` do iOS: aparência de repouso inicial.
     * Light: bg branco e borda = [borderColorOverride] (default `DSSColors.primary`).
     */
    private fun setupAppearance() {
        val isDarkOrBlack = ThemeManager.colorScheme == ColorScheme.DARK ||
            ThemeManager.colorScheme == ColorScheme.BLACK
        if (isDarkOrBlack) {
            // iOS `setupButton` (dark): bg = base, borda branca 40%; sem ramo de isSelected.
            val baseBackground = Color.rgb(28, 28, 30)
            val border = Color.argb((0.4f * 255).toInt(), 255, 255, 255)
            background = DrawableFactory.rounded(
                context = context,
                backgroundColor = baseBackground,
                cornerRadiusDp = 20f,
                strokeColor = border,
                strokeWidthDp = 1f,
            )
            // iOS: setTitleColor(.white, .normal) / setTitleColor(.black, .selected).
            setTextColor(if (isSelected) Color.BLACK else Color.WHITE)
        } else {
            // iOS `setupButton` (light): backgroundColor = .white, borda = borderColor; sem ramo de isSelected.
            background = DrawableFactory.rounded(
                context = context,
                backgroundColor = Color.WHITE,
                cornerRadiusDp = 20f,
                strokeColor = borderColorOverride,
                strokeWidthDp = 1f,
            )
            // iOS: setTitleColor(unselectedTitleColor, .normal) / setTitleColor(selectedTitleColor, .selected).
            setTextColor(if (isSelected) selectedTitleColor else unselectedTitleColor)
        }
    }

    /**
     * Espelha `updateAppearance` do iOS: reação à mudança de seleção.
     * Light: selecionado = primary; não-selecionado = transparente com borda darkGray.
     */
    private fun updateAppearance() {
        val isDarkOrBlack = ThemeManager.colorScheme == ColorScheme.DARK ||
            ThemeManager.colorScheme == ColorScheme.BLACK
        if (isDarkOrBlack) {
            val baseBackground = Color.rgb(28, 28, 30)
            val bg = if (isSelected) Color.WHITE else baseBackground
            val border = Color.argb((0.4f * 255).toInt(), 255, 255, 255)
            background = DrawableFactory.rounded(
                context = context,
                backgroundColor = bg,
                cornerRadiusDp = 20f,
                strokeColor = border,
                strokeWidthDp = 1f,
            )
            setTextColor(if (isSelected) Color.BLACK else Color.WHITE)
        } else {
            val bg = if (isSelected) DSSColors.primary() else Color.TRANSPARENT
            val border = if (isSelected) DSSColors.primary() else Color.DKGRAY
            background = DrawableFactory.rounded(
                context = context,
                backgroundColor = bg,
                cornerRadiusDp = 20f,
                strokeColor = border,
                strokeWidthDp = 1f,
            )
            setTextColor(if (isSelected) selectedTitleColor else unselectedTitleColor)
        }
    }
}
