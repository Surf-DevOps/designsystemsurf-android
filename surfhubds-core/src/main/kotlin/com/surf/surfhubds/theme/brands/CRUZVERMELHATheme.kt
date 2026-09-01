package com.surf.surfhubds.theme.brands

import com.surf.surfhubds.theme.DefaultTheme
import com.surf.surfhubds.theme.colorValueWithAlpha
import com.surf.surfhubds.tokens.ColorTokens
import com.surf.surfhubds.tokens.ColorValue

// Gerado pelo brand-builder — espelho do CRUZVERMELHATheme.swift (iOS).
class CRUZVERMELHATheme : DefaultTheme() {
    override fun buildColors(): ColorTokens = ColorTokens(
        primary = ColorValue.fromHex("#D90915", "#D90915"),
        secondary = ColorValue.fromHex("#FFFFFF", "#FFFFFF"),
        primaryButton = ColorValue.fromHex("#D90915", "#D90915"),
        buttonText = ColorValue.fromHex("#FFFFFF", "#FFFFFF"),
        surface = ColorValue.fromHex("#FFFFFF", "#1C1C1E"),
        background = ColorValue.fromHex("#FFFFFF", "#000000"),
        backgroundSecondary = ColorValue.fromHex("#FFFFFF", "#1C1C1E"),
        error = ColorValue.fromHex("#DC3545", "#E74C3C"),
        success = ColorValue.fromHex("#28A745", "#4CAF50"),
        textPrimary = ColorValue.fromHex("#212121", "#FFFFFF"),
        textSecondary = ColorValue.fromHex("#757575", "#B0B0B0"),
        textTertiary = ColorValue.fromHex("#9E9E9E", "#808080"),
        textLink = ColorValue.fromHex("#1D4ED8", "#0A84FF"),
        textOnPrimary = ColorValue.fromHex("#FFFFFF", "#FFFFFF"),
        borderDefault = ColorValue.fromHex("#595959", "#FFFFFF"),
        borderFocus = ColorValue.fromHex("#EB0033", "#EA1E2C"),
        borderError = ColorValue.fromHex("#DC3545", "#E74C3C"),
        overlay = colorValueWithAlpha("#000000", "#000000", 0.5f, 0.7f),
        divider = colorValueWithAlpha("#E0E0E0", "#424242", 0.5f, 0.7f),
    )
}
