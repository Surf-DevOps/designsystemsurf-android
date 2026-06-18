package com.surf.surfhubds.theme

import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.tokens.ColorTokens
import com.surf.surfhubds.tokens.ColorValue
import com.surf.surfhubds.tokens.ComponentStyles
import com.surf.surfhubds.tokens.DesignTokens

/**
 * Fallback theme inicial antes do app chamar `ThemeManager.setTheme(...)`.
 * Não deveria aparecer em runtime — se aparecer, app esqueceu de configurar a brand.
 */
internal object NoopTheme : Theme {
    override val colorScheme: ColorScheme = ColorScheme.LIGHT
    override val tokens: DesignTokens = DesignTokens(
        colors = ColorTokens(
            primary = ColorValue.fromHex("#000000"),
            secondary = ColorValue.fromHex("#000000"),
            primaryButton = ColorValue.fromHex("#000000"),
            buttonText = ColorValue.fromHex("#FFFFFF"),
            surface = ColorValue.fromHex("#FFFFFF"),
            background = ColorValue.fromHex("#FFFFFF"),
            backgroundSecondary = ColorValue.fromHex("#F8F8F8"),
            error = ColorValue.fromHex("#DC3545"),
            success = ColorValue.fromHex("#28A745"),
            textPrimary = ColorValue.fromHex("#212121"),
            textSecondary = ColorValue.fromHex("#757575"),
            textTertiary = ColorValue.fromHex("#9E9E9E"),
            textLink = ColorValue.fromHex("#1D4ED8"),
            textOnPrimary = ColorValue.fromHex("#FFFFFF"),
            borderDefault = ColorValue.fromHex("#595959"),
            borderFocus = ColorValue.fromHex("#EB0033"),
            borderError = ColorValue.fromHex("#DC3545"),
            overlay = colorValueWithAlpha("#000000", "#000000", 0.5f, 0.7f),
            divider = colorValueWithAlpha("#E0E0E0", "#424242", 0.5f, 0.7f),
        ),
    )
    // Sem Context disponível (object/fallback que não deveria aparecer em runtime),
    // então a tipografia usa o fallback de system font do DefaultComponentStyles.
    override val components: ComponentStyles = DefaultComponentStyles.create(tokens, context = null)
}
