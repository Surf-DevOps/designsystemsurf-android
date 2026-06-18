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
import com.surf.surfhubds.tokens.FontSpec
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSSecondaryButton` do iOS — botão com borda e fundo transparente.
 *
 * iOS: `init(title:titleColor:fontTitle:borderColor:borderWidth:action:)` com
 * `titleColor = DSSColors.buttonText`, `cornerRadius = 25` e `masksToBounds = true`.
 * `fontTitle`, `borderColor` e `borderWidth` são obrigatórios no iOS (sem default).
 */
class DSSSecondaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatButton(context, attrs, defStyleAttr), ThemeAware {

    var onTap: (() -> Unit)? = null

    /** `titleColor` opcional do iOS; null => usa o token semântico `buttonText` (reage ao tema). */
    @ColorInt var customTitleColor: Int? = null
        set(value) { field = value; refresh() }

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

    /**
     * Configuração equivalente ao `init` do iOS
     * (`title:titleColor:fontTitle:borderColor:borderWidth:action:`).
     *
     * `titleColor`/`borderColor` aceitam `null` para usar os tokens semânticos
     * (`buttonText`/`primary`), reagindo ao tema. No iOS `fontTitle`/`borderColor`/`borderWidth`
     * são obrigatórios; aqui são opcionais para não quebrar a API e preservar os defaults Android.
     */
    fun configure(
        title: String? = null,
        @ColorInt titleColor: Int? = null,
        fontTitle: FontSpec? = null,
        @ColorInt borderColor: Int? = null,
        borderWidthDp: Float = this.borderWidthDp,
        cornerRadiusDp: Float = this.cornerRadiusDp,
        action: (() -> Unit)? = null,
    ) {
        if (title != null) text = title
        customTitleColor = titleColor
        if (fontTitle != null) {
            typeface = fontTitle.typeface
            textSize = fontTitle.sizeSp
        }
        borderColorOverride = borderColor
        this.borderWidthDp = borderWidthDp
        this.cornerRadiusDp = cornerRadiusDp
        onTap = action
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        // iOS: default de titleColor e borderColor = DSSColors.primaryButton. Overrides seguem valendo.
        setBackground(DrawableFactory.rounded(
            context = context,
            backgroundColor = android.graphics.Color.TRANSPARENT,
            cornerRadiusDp = cornerRadiusDp,
            strokeColor = borderColorOverride ?: DSSColors.primaryButton(),
            strokeWidthDp = borderWidthDp,
        ))
        setTextColor(customTitleColor ?: DSSColors.primaryButton())
    }
}
