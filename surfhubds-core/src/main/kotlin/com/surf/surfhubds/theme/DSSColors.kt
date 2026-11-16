package com.surf.surfhubds.theme

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

/**
 * Atalho pros tokens semânticos da brand atual (`ThemeManager.theme`).
 *
 * Equivalente ao `DSSColors` do iOS — toda chamada resolve para a [ColorScheme] atual.
 */
@Suppress("MemberVisibilityCanBePrivate")
object DSSColors {
    private val scheme get() = ThemeManager.colorScheme
    private val c get() = ThemeManager.tokens.colors

    @ColorInt fun primary(): Int = c.primary.resolved(scheme)
    @ColorInt fun secondary(): Int = c.secondary.resolved(scheme)
    @ColorInt fun primaryButton(): Int = c.primaryButton.resolved(scheme)
    @ColorInt fun buttonText(): Int = c.buttonText.resolved(scheme)
    @ColorInt fun surface(): Int = c.surface.resolved(scheme)
    @ColorInt fun background(): Int = c.background.resolved(scheme)
    @ColorInt fun backgroundSecondary(): Int = c.backgroundSecondary.resolved(scheme)
    @ColorInt fun error(): Int = c.error.resolved(scheme)
    @ColorInt fun success(): Int = c.success.resolved(scheme)
    @ColorInt fun textPrimary(): Int = c.textPrimary.resolved(scheme)
    @ColorInt fun textSecondary(): Int = c.textSecondary.resolved(scheme)
    @ColorInt fun textTertiary(): Int = c.textTertiary.resolved(scheme)
    @ColorInt fun textLink(): Int = c.textLink.resolved(scheme)
    @ColorInt fun textOnPrimary(): Int = c.textOnPrimary.resolved(scheme)
    @ColorInt fun borderDefault(): Int = c.borderDefault.resolved(scheme)
    @ColorInt fun borderFocus(): Int = c.borderFocus.resolved(scheme)
    @ColorInt fun borderError(): Int = c.borderError.resolved(scheme)
    @ColorInt fun overlay(): Int = c.overlay.resolved(scheme)
    @ColorInt fun divider(): Int = c.divider.resolved(scheme)

    /**
     * Divisória mais marcada que [divider] (linhas dentro de cards cinza, puxador de sheet).
     * Derivada de [textTertiary] para seguir a brand e o tema; no claro dá ~#E7E7E7.
     */
    @ColorInt fun dividerStrong(): Int = ColorUtils.setAlphaComponent(textTertiary(), 0x40)

    /** Contorno discreto de campos, pílulas e seleção; no claro dá ~#C5C5C5. */
    @ColorInt fun borderSubtle(): Int = ColorUtils.setAlphaComponent(textTertiary(), 0x99)

    /**
     * Cor de conteúdo (texto/ícone/knob) desenhado SOBRE o fill [primary]. No iOS o primary
     * é sempre escuro/saturado, então lá usa-se `.white` fixo; no Android algumas brands
     * deixam o primary BRANCO no dark/black (ex.: CorreiosCelular), e o branco fixo sumiria.
     *
     * Regra: mantém branco (como o iOS) quando o primary é escuro/saturado; só vira escuro
     * quando o primary é claro (luminância alta). Assim não há regressão nas brands de
     * primary colorido e resolve o "branco no branco" das de primary branco.
     */
    @ColorInt fun contrastOnPrimary(): Int = contrastOn(primary())

    /** Branco ou quase-preto, o que contrasta com [background] (limiar de luminância). */
    @ColorInt fun contrastOn(@ColorInt background: Int): Int =
        if (ColorUtils.calculateLuminance(background) > 0.6) Color.BLACK else Color.WHITE

    /**
     * Fundo do header da Home. Delega pro [HomeHeaderBackground] do tema atual (ex.: o glow
     * radial da Chip da Saúde) quando o tema implementa essa interface; senão cai no
     * preenchimento sólido de sempre ([background] no escuro, [primary] no claro) —
     * comportamento idêntico ao que já existia pra todas as brands antes desse hook.
     */
    fun homeHeaderBackground(context: Context): Drawable {
        val theme = ThemeManager.theme
        if (theme is HomeHeaderBackground) return theme.homeHeaderBackground(context)
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return ColorDrawable(if (night) background() else primary())
    }

    /**
     * Cor da status bar enquanto o header da Home (ver [homeHeaderBackground]) está visível.
     * Delega pro [HomeHeaderBackground] do tema atual quando ele implementa essa interface;
     * senão cai em [background] no escuro / [primary] no claro — idêntico a antes desse hook.
     */
    fun homeHeaderStatusBarColor(context: Context): Int {
        val theme = ThemeManager.theme
        if (theme is HomeHeaderBackground) return theme.homeHeaderStatusBarColor(context)
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return if (night) background() else primary()
    }
}
