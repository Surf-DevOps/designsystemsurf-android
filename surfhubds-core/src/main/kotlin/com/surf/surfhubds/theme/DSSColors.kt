package com.surf.surfhubds.theme

import androidx.annotation.ColorInt

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
}
