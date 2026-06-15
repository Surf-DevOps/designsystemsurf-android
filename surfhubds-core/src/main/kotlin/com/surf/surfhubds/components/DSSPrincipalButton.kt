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
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSPrincipalButton` do iOS — botão principal com fill da brand.
 *
 * iOS: `init(title:backgroundColor:textColor:font:cornerRadius:action:)` com
 * `title = "Example"`, `backgroundColor = DSSColors.primary`, `textColor = DSSColors.buttonText`,
 * `font = DSSFont.light(16)`, `cornerRadius = 25` e `defaultSize = 320x50`.
 */
class DSSPrincipalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatButton(context, attrs, defStyleAttr), ThemeAware {

    var onTap: (() -> Unit)? = null

    var cornerRadiusDp: Float = 25f
        set(value) { field = value; refresh() }

    /** Espelha `defaultSize` do iOS (pt -> dp 1:1). Aplicado como min width/height. */
    var defaultWidthDp: Float = 320f
        set(value) { field = value; minWidth = value.dpToPx(context); requestLayout() }

    var defaultHeightDp: Float = 50f
        set(value) { field = value; minHeight = value.dpToPx(context); requestLayout() }

    /** `backgroundColor` opcional do iOS; null => usa o token semântico (reage ao tema). */
    @ColorInt
    var customBackgroundColor: Int? = null
        set(value) { field = value; refresh() }

    /** `textColor` opcional do iOS; null => usa o token semântico (reage ao tema). */
    @ColorInt
    var customTextColor: Int? = null
        set(value) { field = value; refresh() }

    init {
        gravity = Gravity.CENTER
        isAllCaps = false
        textSize = 16f
        typeface = DSSFont.light(context, 16f).typeface
        // Preserva o android:text vindo do XML (já aplicado pelo super). Só usa o
        // placeholder "Example" do iOS quando nenhum texto foi informado.
        if (text.isNullOrBlank()) text = "Example"
        minWidth = defaultWidthDp.dpToPx(context)
        minHeight = defaultHeightDp.dpToPx(context)
        setOnClickListener { onTap?.invoke() }
        refresh()
        setupThemeObserver()
    }

    /**
     * Configuração equivalente ao `init` do iOS. Todos os parâmetros são opcionais
     * e mantêm os mesmos defaults do iOS.
     */
    fun configure(
        title: String = "Example",
        @ColorInt backgroundColor: Int? = null,
        @ColorInt textColor: Int? = null,
        cornerRadiusDp: Float = 25f,
        action: (() -> Unit)? = null,
    ) {
        text = title
        customBackgroundColor = backgroundColor
        customTextColor = textColor
        this.cornerRadiusDp = cornerRadiusDp
        onTap = action
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackground(DrawableFactory.rounded(
            context = context,
            backgroundColor = customBackgroundColor ?: DSSColors.primary(),
            cornerRadiusDp = cornerRadiusDp,
        ))
        setTextColor(customTextColor ?: DSSColors.buttonText())
    }
}
