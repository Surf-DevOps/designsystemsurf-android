package com.surf.surfhubds.tokens

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

data class ColorValue(
    @ColorInt val light: Int,
    @ColorInt val dark: Int = light,
    @ColorInt val black: Int = dark,
) {
    fun resolved(scheme: ColorScheme): Int = when (scheme) {
        ColorScheme.LIGHT -> light
        ColorScheme.DARK -> dark
        ColorScheme.BLACK -> black
    }

    fun withAlpha(alpha: Float): ColorValue = ColorValue(
        light = ColorUtils.setAlphaComponent(light, (alpha * 255).toInt().coerceIn(0, 255)),
        dark = ColorUtils.setAlphaComponent(dark, (alpha * 255).toInt().coerceIn(0, 255)),
        black = ColorUtils.setAlphaComponent(black, (alpha * 255).toInt().coerceIn(0, 255)),
    )

    companion object {
        fun fromHex(light: String, dark: String? = null, black: String? = null) = ColorValue(
            light = Color.parseColor(normalize(light)),
            dark = Color.parseColor(normalize(dark ?: light)),
            black = Color.parseColor(normalize(black ?: dark ?: light)),
        )

        private fun normalize(hex: String): String = if (hex.startsWith("#")) hex else "#$hex"
    }
}
