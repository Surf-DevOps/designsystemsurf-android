package com.surf.surfhubds.theme.brands

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.DefaultTheme
import com.surf.surfhubds.theme.HomeHeaderBackground
import com.surf.surfhubds.theme.RadialGradientDrawable
import com.surf.surfhubds.theme.colorValueWithAlpha
import com.surf.surfhubds.tokens.ColorTokens
import com.surf.surfhubds.tokens.ColorValue

// Gerado pelo brand-builder — espelho do ChipDaSaudeTheme.swift (iOS).
// ATENÇÃO: homeHeaderBackground() foi adicionado manualmente (não vem do brand-builder) —
// se esse arquivo for regenerado, reaplicar esse trecho.
class ChipDaSaudeTheme : DefaultTheme(), HomeHeaderBackground {

    /**
     * Glow radial "Fundo de marca" do guia (pág. 05 · Gradientes): Elétrico concentrado perto
     * do canto superior-esquerdo dissipando pro Navy, em vez do preenchimento sólido padrão —
     * ver [RadialGradientDrawable]. No escuro mantém sólido (já cai em Navy).
     */
    override fun homeHeaderBackground(context: Context): Drawable {
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        if (night) return ColorDrawable(DSSColors.background())
        return RadialGradientDrawable(
            centerColor = 0xFF1494F0.toInt(),
            edgeColor = 0xFF002173.toInt(),
            centerXFraction = 0.15f,
            centerYFraction = 0.05f,
            radiusFraction = 0.5f,
        )
    }

    /** Elétrico — cor do início do glow em [homeHeaderBackground], não o Azul Chip de sempre. */
    override fun homeHeaderStatusBarColor(context: Context): Int {
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return if (night) DSSColors.background() else 0xFF1494F0.toInt()
    }

    override fun buildColors(): ColorTokens = ColorTokens(
        primary = ColorValue.fromHex("#0A6FD6", "#1494F0"),
        secondary = ColorValue.fromHex("#1494F0", "#0A6FD6"),
        primaryButton = ColorValue.fromHex("#0A6FD6", "#1494F0"),
        buttonText = ColorValue.fromHex("#FFFFFF", "#FFFFFF"),
        surface = ColorValue.fromHex("#FFFFFF", "#1C1C1E"),
        background = ColorValue.fromHex("#FFFFFF", "#002173"),
        backgroundSecondary = ColorValue.fromHex("#EAF3FF", "#002884"),
        error = ColorValue.fromHex("#DC3545", "#E74C3C"),
        success = ColorValue.fromHex("#28A745", "#4CAF50"),
        textPrimary = ColorValue.fromHex("#002173", "#FFFFFF"),
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
