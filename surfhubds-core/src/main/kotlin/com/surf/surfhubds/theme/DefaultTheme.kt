package com.surf.surfhubds.theme

import androidx.core.graphics.ColorUtils
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.tokens.ColorTokens
import com.surf.surfhubds.tokens.ColorValue
import com.surf.surfhubds.tokens.ComponentStyles
import com.surf.surfhubds.tokens.DesignTokens

internal fun argbHex(hex: String, alpha: Float): Int {
    val base = android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
    return ColorUtils.setAlphaComponent(base, (alpha * 255).toInt().coerceIn(0, 255))
}

internal fun colorValueWithAlpha(
    lightHex: String,
    darkHex: String,
    lightAlpha: Float,
    darkAlpha: Float,
): ColorValue = ColorValue(
    light = argbHex(lightHex, lightAlpha),
    dark = argbHex(darkHex, darkAlpha),
)

/**
 * Equivalente ao `DefaultTheme.swift`. Brand themes herdam de [BrandTheme] e
 * sobrescrevem apenas [buildColors].
 */
open class DefaultTheme : Theme {

    final override val colorScheme: ColorScheme = ColorScheme.LIGHT

    final override val tokens: DesignTokens by lazy {
        DesignTokens(colors = buildColors())
    }

    final override val components: ComponentStyles by lazy {
        DefaultComponentStyles.create(tokens)
    }

    protected open fun buildColors(): ColorTokens = ColorTokens(
        primary = ColorValue.fromHex("#1D4ED8", "#0A84FF"),
        secondary = ColorValue.fromHex("#5856D6", "#5E5CE6"),
        primaryButton = ColorValue.fromHex("#1D4ED8", "#0A84FF"),
        buttonText = ColorValue.fromHex("#FFFFFF", "#000000"),
        surface = ColorValue.fromHex("#FFFFFF", "#1C1C1E"),
        background = ColorValue.fromHex("#F2F2F7", "#000000"),
        backgroundSecondary = ColorValue.fromHex("#F8F8F8", "#1C1C1E"),
        error = ColorValue.fromHex("#DC3545", "#E74C3C"),
        success = ColorValue.fromHex("#28A745", "#4CAF50"),
        textPrimary = ColorValue.fromHex("#111827", "#F9FAFB"),
        textSecondary = ColorValue.fromHex("#6B7280", "#B0B0B0"),
        textTertiary = ColorValue.fromHex("#9E9E9E", "#808080"),
        textLink = ColorValue.fromHex("#1D4ED8", "#0A84FF"),
        textOnPrimary = ColorValue.fromHex("#FFFFFF", "#FFFFFF"),
        borderDefault = ColorValue.fromHex("#595959", "#FFFFFF"),
        borderFocus = ColorValue.fromHex("#EB0033", "#EA1E2C"),
        borderError = ColorValue.fromHex("#DC3545", "#E74C3C"),
        overlay = colorValueWithAlpha("#000000", "#000000", 0.4f, 0.6f),
        divider = colorValueWithAlpha("#C6C6C8", "#38383A", 0.35f, 0.65f),
    )
}
