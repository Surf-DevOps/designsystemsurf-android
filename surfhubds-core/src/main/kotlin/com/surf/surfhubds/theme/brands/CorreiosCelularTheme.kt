package com.surf.surfhubds.theme.brands

import com.surf.surfhubds.theme.DefaultTheme
import com.surf.surfhubds.theme.colorValueWithAlpha
import com.surf.surfhubds.tokens.ColorTokens
import com.surf.surfhubds.tokens.ColorValue

class CorreiosCelularTheme : DefaultTheme() {
    override fun buildColors(): ColorTokens = ColorTokens(
        primary = ColorValue.fromHex("#00416B", "#FFFFFF"),
        secondary = ColorValue.fromHex("#FFD400", "#FFFFFF"),
        primaryButton = ColorValue.fromHex("#00416B", "#FFFFFF"),
        // dark/black: primary/primaryButton viram branco -> conteúdo precisa ser escuro
        // (antes #FFFFFF = branco no branco). Azul da marca contrasta no botão branco.
        buttonText = ColorValue.fromHex("#FFD400", "#00416B", "#00416B"),
        surface = ColorValue.fromHex("#FFFFFF", "#1C1C1E"),
        background = ColorValue.fromHex("#FFFFFF", "#FFFFFF"),
        backgroundSecondary = ColorValue.fromHex("#F8F8F8", "#FFFFFF"),
        error = ColorValue.fromHex("#DC3545", "#E74C3C"),
        success = ColorValue.fromHex("#28A745", "#4CAF50"),
        textPrimary = ColorValue.fromHex("#000000", "#FFFFFF"),
        textSecondary = ColorValue.fromHex("#757575", "#B0B0B0"),
        textTertiary = ColorValue.fromHex("#9E9E9E", "#808080"),
        textLink = ColorValue.fromHex("#1D4ED8", "#0A84FF"),
        textOnPrimary = ColorValue.fromHex("#FFFFFF", "#00416B", "#00416B"),
        borderDefault = ColorValue.fromHex("#595959", "#FFFFFF"),
        borderFocus = ColorValue.fromHex("#EB0033", "#EA1E2C"),
        borderError = ColorValue.fromHex("#DC3545", "#E74C3C"),
        overlay = colorValueWithAlpha("#000000", "#000000", 0.5f, 0.7f),
        divider = colorValueWithAlpha("#E0E0E0", "#424242", 0.5f, 0.7f),
    )
}
