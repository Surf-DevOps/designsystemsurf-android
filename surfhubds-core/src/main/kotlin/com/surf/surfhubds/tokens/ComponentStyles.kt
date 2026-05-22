package com.surf.surfhubds.tokens

import android.graphics.Typeface

data class ComponentStyles(
    val button: ButtonStyles,
    val textField: TextFieldStyles,
    val card: CardStyles,
    val navigation: NavigationStyles,
    val dialog: DialogStyles,
    val list: ListStyles,
    val chip: ChipStyles,
    val toggle: ToggleStyles,
    val progress: ProgressStyles,
    val badge: BadgeStyles,
)

/** Insets em DP. */
data class EdgeInsets(val top: Float, val left: Float, val bottom: Float, val right: Float) {
    companion object {
        val Zero = EdgeInsets(0f, 0f, 0f, 0f)
        fun all(value: Float) = EdgeInsets(value, value, value, value)
        fun symmetric(vertical: Float, horizontal: Float) =
            EdgeInsets(vertical, horizontal, vertical, horizontal)
    }
}

/** Size em DP. */
data class Size(val width: Float, val height: Float)

data class FontSpec(val typeface: Typeface, val sizeSp: Float)

data class TransformSpec(val scaleX: Float = 1f, val scaleY: Float = 1f)

// Button --------------------------------------------------------------------

data class ButtonStyles(
    val primary: ButtonStyle,
    val secondary: ButtonStyle,
    val tertiary: ButtonStyle,
    val danger: ButtonStyle,
    val ghost: ButtonStyle,
    val link: ButtonStyle,
)

data class ButtonStyle(
    val backgroundColor: ColorValue,
    val textColor: ColorValue,
    val borderColor: ColorValue?,
    val borderWidthDp: Float,
    val cornerRadiusDp: Float,
    val font: FontSpec,
    val contentInsets: EdgeInsets,
    val minHeightDp: Float,
    val shadow: ShadowStyle?,
    val pressed: ButtonStateStyle? = null,
    val disabled: ButtonStateStyle? = null,
)

data class ButtonStateStyle(
    val backgroundColor: ColorValue? = null,
    val textColor: ColorValue? = null,
    val borderColor: ColorValue? = null,
    val transform: TransformSpec? = null,
)

// TextField -----------------------------------------------------------------

data class TextFieldStyles(
    val default: TextFieldStyle,
    val filled: TextFieldStyle,
    val outlined: TextFieldStyle,
)

data class TextFieldStyle(
    val backgroundColor: ColorValue,
    val textColor: ColorValue,
    val placeholderColor: ColorValue,
    val borderColor: ColorValue,
    val borderWidthDp: Float,
    val cornerRadiusDp: Float,
    val font: FontSpec,
    val contentInsets: EdgeInsets,
    val minHeightDp: Float,
    val focus: TextFieldStateStyle? = null,
    val error: TextFieldStateStyle? = null,
    val disabled: TextFieldStateStyle? = null,
)

data class TextFieldStateStyle(
    val backgroundColor: ColorValue? = null,
    val borderColor: ColorValue? = null,
    val borderWidthDp: Float? = null,
)

// Card ----------------------------------------------------------------------

data class CardStyles(val elevated: CardStyle, val filled: CardStyle, val outlined: CardStyle)

data class CardStyle(
    val backgroundColor: ColorValue,
    val cornerRadiusDp: Float,
    val padding: EdgeInsets,
    val shadow: ShadowStyle?,
    val borderColor: ColorValue?,
    val borderWidthDp: Float,
)

// Navigation ----------------------------------------------------------------

data class NavigationStyles(val bar: NavigationBarStyle, val tab: TabBarStyle)

data class NavigationBarStyle(
    val backgroundColor: ColorValue,
    val tintColor: ColorValue,
    val titleFont: FontSpec,
    val shadow: ShadowStyle?,
    val heightDp: Float,
)

data class TabBarStyle(
    val backgroundColor: ColorValue,
    val selectedColor: ColorValue,
    val unselectedColor: ColorValue,
    val indicatorColor: ColorValue?,
    val font: FontSpec,
    val shadow: ShadowStyle?,
)

// Dialog --------------------------------------------------------------------

data class DialogStyles(val alert: DialogStyle, val bottomSheet: DialogStyle, val fullScreen: DialogStyle)

data class DialogStyle(
    val backgroundColor: ColorValue,
    val cornerRadiusDp: Float,
    val padding: EdgeInsets,
    val shadow: ShadowStyle?,
    val overlayColor: ColorValue,
    val maxWidthDp: Float?,
)

// List ----------------------------------------------------------------------

data class ListStyles(val item: ListItemStyle, val section: ListSectionStyle, val separator: ListSeparatorStyle)

data class ListItemStyle(
    val backgroundColor: ColorValue,
    val selectedBackgroundColor: ColorValue,
    val textColor: ColorValue,
    val secondaryTextColor: ColorValue,
    val padding: EdgeInsets,
    val minHeightDp: Float,
)

data class ListSectionStyle(
    val headerFont: FontSpec,
    val headerTextColor: ColorValue,
    val headerPadding: EdgeInsets,
    val footerFont: FontSpec,
    val footerTextColor: ColorValue,
    val footerPadding: EdgeInsets,
)

data class ListSeparatorStyle(
    val color: ColorValue,
    val thicknessDp: Float,
    val insets: EdgeInsets,
)

// Chip ----------------------------------------------------------------------

data class ChipStyles(val filled: ChipStyle, val outlined: ChipStyle)

data class ChipStyle(
    val backgroundColor: ColorValue,
    val textColor: ColorValue,
    val borderColor: ColorValue?,
    val borderWidthDp: Float,
    val cornerRadiusDp: Float,
    val font: FontSpec,
    val contentInsets: EdgeInsets,
    val minHeightDp: Float,
)

// Toggle --------------------------------------------------------------------

data class ToggleStyles(val switchStyle: SwitchStyle, val checkbox: CheckboxStyle, val radio: RadioStyle)

data class SwitchStyle(
    val trackColor: ColorValue,
    val trackColorOn: ColorValue,
    val thumbColor: ColorValue,
    val thumbColorOn: ColorValue,
    val size: Size,
)

data class CheckboxStyle(
    val borderColor: ColorValue,
    val backgroundColor: ColorValue,
    val checkColor: ColorValue,
    val cornerRadiusDp: Float,
    val size: Size,
)

data class RadioStyle(
    val borderColor: ColorValue,
    val backgroundColor: ColorValue,
    val dotColor: ColorValue,
    val size: Size,
)

// Progress ------------------------------------------------------------------

data class ProgressStyles(val linear: ProgressStyle, val circular: ProgressStyle)

data class ProgressStyle(
    val trackColor: ColorValue,
    val progressColor: ColorValue,
    val thicknessDp: Float,
)

// Badge ---------------------------------------------------------------------

data class BadgeStyles(val standard: BadgeStyle, val dot: BadgeStyle)

data class BadgeStyle(
    val backgroundColor: ColorValue,
    val textColor: ColorValue,
    val font: FontSpec,
    val cornerRadiusDp: Float,
    val padding: EdgeInsets,
    val minSize: Size,
)
