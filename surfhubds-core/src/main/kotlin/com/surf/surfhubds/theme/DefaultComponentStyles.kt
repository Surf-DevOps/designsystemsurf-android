package com.surf.surfhubds.theme

import android.content.Context
import android.graphics.Typeface
import com.surf.surfhubds.tokens.BadgeStyle
import com.surf.surfhubds.tokens.BadgeStyles
import com.surf.surfhubds.tokens.ButtonStateStyle
import com.surf.surfhubds.tokens.ButtonStyle
import com.surf.surfhubds.tokens.ButtonStyles
import com.surf.surfhubds.tokens.CardStyle
import com.surf.surfhubds.tokens.CardStyles
import com.surf.surfhubds.tokens.CheckboxStyle
import com.surf.surfhubds.tokens.ChipStyle
import com.surf.surfhubds.tokens.ChipStyles
import com.surf.surfhubds.tokens.ColorValue
import com.surf.surfhubds.tokens.ComponentStyles
import com.surf.surfhubds.tokens.DesignTokens
import com.surf.surfhubds.tokens.DialogStyle
import com.surf.surfhubds.tokens.DialogStyles
import com.surf.surfhubds.tokens.EdgeInsets
import com.surf.surfhubds.tokens.FontSpec
import com.surf.surfhubds.tokens.ListItemStyle
import com.surf.surfhubds.tokens.ListSectionStyle
import com.surf.surfhubds.tokens.ListSeparatorStyle
import com.surf.surfhubds.tokens.ListStyles
import com.surf.surfhubds.tokens.NavigationBarStyle
import com.surf.surfhubds.tokens.NavigationStyles
import com.surf.surfhubds.tokens.ProgressStyle
import com.surf.surfhubds.tokens.ProgressStyles
import com.surf.surfhubds.tokens.RadioStyle
import com.surf.surfhubds.tokens.Size
import com.surf.surfhubds.tokens.SwitchStyle
import com.surf.surfhubds.tokens.TabBarStyle
import com.surf.surfhubds.tokens.TextFieldStateStyle
import com.surf.surfhubds.tokens.TextFieldStyle
import com.surf.surfhubds.tokens.TextFieldStyles
import com.surf.surfhubds.tokens.ToggleStyles
import com.surf.surfhubds.tokens.TransformSpec

/**
 * Reconstrução em Kotlin dos `createButtonStyles`/`createCardStyles` etc do `DefaultTheme.swift`.
 * Toda brand reusa este builder e só sobrescreve [DesignTokens.colors].
 */
object DefaultComponentStyles {

    private val systemTypefaceRegular = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val systemTypefaceMedium = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val systemTypefaceBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    private fun font(sizeSp: Float, bold: Boolean = false): FontSpec =
        FontSpec(if (bold) systemTypefaceBold else systemTypefaceRegular, sizeSp)

    fun create(tokens: DesignTokens, context: Context? = null): ComponentStyles {
        val colors = tokens.colors
        val spacing = tokens.spacing
        val radius = tokens.radius
        val shadows = tokens.shadows

        return ComponentStyles(
            button = ButtonStyles(
                primary = ButtonStyle(
                    backgroundColor = colors.primary,
                    textColor = colors.textOnPrimary,
                    borderColor = null,
                    borderWidthDp = 0f,
                    cornerRadiusDp = radius.md,
                    font = font(17f, bold = true),
                    contentInsets = EdgeInsets(spacing.sm, spacing.lg, spacing.sm, spacing.lg),
                    minHeightDp = 44f,
                    shadow = shadows.sm,
                    pressed = ButtonStateStyle(
                        backgroundColor = colors.primary.withAlpha(0.9f),
                        transform = TransformSpec(0.98f, 0.98f),
                    ),
                    disabled = ButtonStateStyle(
                        backgroundColor = colors.textTertiary,
                        textColor = colors.textSecondary,
                    ),
                ),
                secondary = ButtonStyle(
                    backgroundColor = colors.surface,
                    textColor = colors.primary,
                    borderColor = colors.primary,
                    borderWidthDp = 2f,
                    cornerRadiusDp = radius.md,
                    font = font(17f, bold = true),
                    contentInsets = EdgeInsets(spacing.sm, spacing.lg, spacing.sm, spacing.lg),
                    minHeightDp = 44f,
                    shadow = null,
                    pressed = ButtonStateStyle(
                        backgroundColor = colors.primary.withAlpha(0.1f),
                        transform = TransformSpec(0.98f, 0.98f),
                    ),
                    disabled = ButtonStateStyle(
                        textColor = colors.textTertiary,
                        borderColor = colors.textTertiary,
                    ),
                ),
                tertiary = ButtonStyle(
                    backgroundColor = colors.textTertiary.withAlpha(0.2f),
                    textColor = colors.textPrimary,
                    borderColor = null,
                    borderWidthDp = 0f,
                    cornerRadiusDp = radius.md,
                    font = font(17f, bold = true),
                    contentInsets = EdgeInsets(spacing.sm, spacing.lg, spacing.sm, spacing.lg),
                    minHeightDp = 44f,
                    shadow = null,
                    pressed = ButtonStateStyle(
                        backgroundColor = colors.textTertiary.withAlpha(0.3f),
                        transform = TransformSpec(0.98f, 0.98f),
                    ),
                    disabled = ButtonStateStyle(
                        backgroundColor = colors.textTertiary.withAlpha(0.1f),
                        textColor = colors.textTertiary,
                    ),
                ),
                danger = ButtonStyle(
                    backgroundColor = colors.error,
                    textColor = colors.textOnPrimary,
                    borderColor = null,
                    borderWidthDp = 0f,
                    cornerRadiusDp = radius.md,
                    font = font(17f, bold = true),
                    contentInsets = EdgeInsets(spacing.sm, spacing.lg, spacing.sm, spacing.lg),
                    minHeightDp = 44f,
                    shadow = shadows.sm,
                    pressed = ButtonStateStyle(
                        backgroundColor = colors.error.withAlpha(0.9f),
                        transform = TransformSpec(0.98f, 0.98f),
                    ),
                    disabled = ButtonStateStyle(
                        backgroundColor = colors.error.withAlpha(0.3f),
                        textColor = colors.textOnPrimary.withAlpha(0.6f),
                    ),
                ),
                ghost = ButtonStyle(
                    backgroundColor = ColorValue.fromHex("#00000000"),
                    textColor = colors.primary,
                    borderColor = null,
                    borderWidthDp = 0f,
                    cornerRadiusDp = radius.md,
                    font = font(17f, bold = true),
                    contentInsets = EdgeInsets(spacing.sm, spacing.lg, spacing.sm, spacing.lg),
                    minHeightDp = 44f,
                    shadow = null,
                    pressed = ButtonStateStyle(
                        backgroundColor = colors.primary.withAlpha(0.1f),
                        transform = TransformSpec(0.98f, 0.98f),
                    ),
                    disabled = ButtonStateStyle(textColor = colors.textTertiary),
                ),
                link = ButtonStyle(
                    backgroundColor = ColorValue.fromHex("#00000000"),
                    textColor = colors.primary,
                    borderColor = null,
                    borderWidthDp = 0f,
                    cornerRadiusDp = 0f,
                    font = font(17f),
                    contentInsets = EdgeInsets(spacing.xxs, spacing.xxs, spacing.xxs, spacing.xxs),
                    minHeightDp = 22f,
                    shadow = null,
                    pressed = ButtonStateStyle(textColor = colors.primary.withAlpha(0.7f)),
                    disabled = ButtonStateStyle(textColor = colors.textTertiary),
                ),
            ),
            textField = TextFieldStyles(
                default = TextFieldStyle(
                    backgroundColor = ColorValue.fromHex("#00000000"),
                    textColor = colors.textPrimary,
                    placeholderColor = colors.textTertiary,
                    borderColor = colors.borderDefault,
                    borderWidthDp = 0f,
                    cornerRadiusDp = 0f,
                    font = font(17f),
                    contentInsets = EdgeInsets(spacing.sm, 0f, spacing.sm, 0f),
                    minHeightDp = 44f,
                    focus = TextFieldStateStyle(borderColor = colors.borderFocus, borderWidthDp = 2f),
                    error = TextFieldStateStyle(borderColor = colors.borderError, borderWidthDp = 2f),
                    disabled = TextFieldStateStyle(backgroundColor = colors.textTertiary.withAlpha(0.1f)),
                ),
                filled = TextFieldStyle(
                    backgroundColor = colors.textTertiary.withAlpha(0.1f),
                    textColor = colors.textPrimary,
                    placeholderColor = colors.textTertiary,
                    borderColor = ColorValue.fromHex("#00000000"),
                    borderWidthDp = 0f,
                    cornerRadiusDp = radius.sm,
                    font = font(17f),
                    contentInsets = EdgeInsets(spacing.sm, spacing.md, spacing.sm, spacing.md),
                    minHeightDp = 44f,
                    focus = TextFieldStateStyle(backgroundColor = colors.primary.withAlpha(0.05f)),
                    error = TextFieldStateStyle(backgroundColor = colors.error.withAlpha(0.05f)),
                    disabled = TextFieldStateStyle(backgroundColor = colors.textTertiary.withAlpha(0.05f)),
                ),
                outlined = TextFieldStyle(
                    backgroundColor = colors.surface,
                    textColor = colors.textPrimary,
                    placeholderColor = colors.textTertiary,
                    borderColor = colors.borderDefault,
                    borderWidthDp = 1f,
                    cornerRadiusDp = radius.sm,
                    font = font(17f),
                    contentInsets = EdgeInsets(spacing.sm, spacing.md, spacing.sm, spacing.md),
                    minHeightDp = 44f,
                    focus = TextFieldStateStyle(borderColor = colors.borderFocus, borderWidthDp = 2f),
                    error = TextFieldStateStyle(borderColor = colors.borderError, borderWidthDp = 2f),
                    disabled = TextFieldStateStyle(
                        backgroundColor = colors.textTertiary.withAlpha(0.05f),
                        borderColor = colors.textTertiary,
                        borderWidthDp = 1f,
                    ),
                ),
            ),
            card = CardStyles(
                elevated = CardStyle(
                    backgroundColor = colors.surface,
                    cornerRadiusDp = radius.lg,
                    padding = EdgeInsets(spacing.md, spacing.md, spacing.md, spacing.md),
                    shadow = shadows.md,
                    borderColor = null,
                    borderWidthDp = 0f,
                ),
                filled = CardStyle(
                    backgroundColor = colors.surface,
                    cornerRadiusDp = radius.lg,
                    padding = EdgeInsets(spacing.md, spacing.md, spacing.md, spacing.md),
                    shadow = null,
                    borderColor = null,
                    borderWidthDp = 0f,
                ),
                outlined = CardStyle(
                    backgroundColor = colors.surface,
                    cornerRadiusDp = radius.lg,
                    padding = EdgeInsets(spacing.md, spacing.md, spacing.md, spacing.md),
                    shadow = null,
                    borderColor = colors.borderDefault,
                    borderWidthDp = 1f,
                ),
            ),
            navigation = NavigationStyles(
                bar = NavigationBarStyle(
                    backgroundColor = colors.surface,
                    tintColor = colors.primary,
                    titleFont = font(18f, bold = true),
                    shadow = shadows.sm,
                    heightDp = 44f,
                ),
                tab = TabBarStyle(
                    backgroundColor = colors.surface,
                    selectedColor = colors.primary,
                    unselectedColor = colors.textTertiary,
                    indicatorColor = colors.primary,
                    font = font(13f, bold = true),
                    shadow = shadows.sm,
                ),
            ),
            dialog = DialogStyles(
                alert = DialogStyle(
                    backgroundColor = colors.surface,
                    cornerRadiusDp = radius.lg,
                    padding = EdgeInsets(spacing.lg, spacing.lg, spacing.lg, spacing.lg),
                    shadow = shadows.xl,
                    overlayColor = colors.overlay,
                    maxWidthDp = 280f,
                ),
                bottomSheet = DialogStyle(
                    backgroundColor = colors.surface,
                    cornerRadiusDp = radius.xl,
                    padding = EdgeInsets(spacing.lg, spacing.lg, spacing.lg, spacing.lg),
                    shadow = shadows.xl,
                    overlayColor = colors.overlay,
                    maxWidthDp = null,
                ),
                fullScreen = DialogStyle(
                    backgroundColor = colors.background,
                    cornerRadiusDp = 0f,
                    padding = EdgeInsets(spacing.lg, spacing.lg, spacing.lg, spacing.lg),
                    shadow = null,
                    overlayColor = colors.overlay,
                    maxWidthDp = null,
                ),
            ),
            list = ListStyles(
                item = ListItemStyle(
                    backgroundColor = colors.surface,
                    selectedBackgroundColor = colors.primary.withAlpha(0.1f),
                    textColor = colors.textPrimary,
                    secondaryTextColor = colors.textSecondary,
                    padding = EdgeInsets(spacing.sm, spacing.md, spacing.sm, spacing.md),
                    minHeightDp = 44f,
                ),
                section = ListSectionStyle(
                    headerFont = font(13f, bold = true),
                    headerTextColor = colors.textSecondary,
                    headerPadding = EdgeInsets(spacing.md, spacing.md, spacing.xs, spacing.md),
                    footerFont = font(11f),
                    footerTextColor = colors.textTertiary,
                    footerPadding = EdgeInsets(spacing.xs, spacing.md, spacing.md, spacing.md),
                ),
                separator = ListSeparatorStyle(
                    color = colors.divider,
                    thicknessDp = 0.5f,
                    insets = EdgeInsets(0f, spacing.md, 0f, 0f),
                ),
            ),
            chip = ChipStyles(
                filled = ChipStyle(
                    backgroundColor = colors.primary.withAlpha(0.2f),
                    textColor = colors.primary,
                    borderColor = null,
                    borderWidthDp = 0f,
                    cornerRadiusDp = radius.full,
                    font = font(13f, bold = true),
                    contentInsets = EdgeInsets(spacing.xs, spacing.sm, spacing.xs, spacing.sm),
                    minHeightDp = 28f,
                ),
                outlined = ChipStyle(
                    backgroundColor = ColorValue.fromHex("#00000000"),
                    textColor = colors.primary,
                    borderColor = colors.primary,
                    borderWidthDp = 1f,
                    cornerRadiusDp = radius.full,
                    font = font(13f, bold = true),
                    contentInsets = EdgeInsets(spacing.xs, spacing.sm, spacing.xs, spacing.sm),
                    minHeightDp = 28f,
                ),
            ),
            toggle = ToggleStyles(
                switchStyle = SwitchStyle(
                    trackColor = colors.textTertiary,
                    trackColorOn = colors.primary,
                    thumbColor = colors.surface,
                    thumbColorOn = colors.surface,
                    size = Size(51f, 31f),
                ),
                checkbox = CheckboxStyle(
                    borderColor = colors.borderDefault,
                    backgroundColor = ColorValue.fromHex("#00000000"),
                    checkColor = colors.primary,
                    cornerRadiusDp = 4f,
                    size = Size(20f, 20f),
                ),
                radio = RadioStyle(
                    borderColor = colors.borderDefault,
                    backgroundColor = ColorValue.fromHex("#00000000"),
                    dotColor = colors.primary,
                    size = Size(20f, 20f),
                ),
            ),
            progress = ProgressStyles(
                linear = ProgressStyle(
                    trackColor = colors.textTertiary.withAlpha(0.3f),
                    progressColor = colors.primary,
                    thicknessDp = 4f,
                ),
                circular = ProgressStyle(
                    trackColor = colors.textTertiary.withAlpha(0.3f),
                    progressColor = colors.primary,
                    thicknessDp = 4f,
                ),
            ),
            badge = BadgeStyles(
                standard = BadgeStyle(
                    backgroundColor = colors.error,
                    textColor = colors.textOnPrimary,
                    font = font(11f),
                    cornerRadiusDp = radius.full,
                    padding = EdgeInsets(spacing.xxxs, spacing.xs, spacing.xxxs, spacing.xs),
                    minSize = Size(18f, 18f),
                ),
                dot = BadgeStyle(
                    backgroundColor = colors.error,
                    textColor = ColorValue.fromHex("#00000000"),
                    font = font(11f),
                    cornerRadiusDp = radius.full,
                    padding = EdgeInsets.Zero,
                    minSize = Size(8f, 8f),
                ),
            ),
        )
    }
}
