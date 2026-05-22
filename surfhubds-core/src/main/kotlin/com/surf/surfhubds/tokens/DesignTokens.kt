package com.surf.surfhubds.tokens

import android.graphics.Color
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

data class DesignTokens(
    val colors: ColorTokens,
    val spacing: SpacingTokens = SpacingTokens(),
    val radius: RadiusTokens = RadiusTokens(),
    val shadows: ShadowTokens = ShadowTokens.default(),
    val animation: AnimationTokens = AnimationTokens(),
)

data class ColorTokens(
    val primary: ColorValue,
    val secondary: ColorValue,
    val primaryButton: ColorValue,
    val buttonText: ColorValue,
    val surface: ColorValue,
    val background: ColorValue,
    val backgroundSecondary: ColorValue,
    val error: ColorValue,
    val success: ColorValue,
    val textPrimary: ColorValue,
    val textSecondary: ColorValue,
    val textTertiary: ColorValue,
    val textLink: ColorValue,
    val textOnPrimary: ColorValue,
    val borderDefault: ColorValue,
    val borderFocus: ColorValue,
    val borderError: ColorValue,
    val overlay: ColorValue,
    val divider: ColorValue,
)

/**
 * Spacing tokens em DP. Conversão para pixels é feita pelos componentes via Resources.displayMetrics.
 */
data class SpacingTokens(
    val none: Float = 0f,
    val xxxs: Float = 2f,
    val xxs: Float = 4f,
    val xs: Float = 8f,
    val sm: Float = 12f,
    val md: Float = 16f,
    val lg: Float = 24f,
    val xl: Float = 32f,
    val xxl: Float = 40f,
    val xxxl: Float = 48f,
    val xxxxl: Float = 64f,
)

/**
 * Corner radius em DP.
 */
data class RadiusTokens(
    val none: Float = 0f,
    val xs: Float = 4f,
    val sm: Float = 8f,
    val md: Float = 12f,
    val lg: Float = 16f,
    val xl: Float = 20f,
    val xxl: Float = 24f,
    val full: Float = 9999f,
)

data class ShadowTokens(
    val none: ShadowStyle,
    val sm: ShadowStyle,
    val md: ShadowStyle,
    val lg: ShadowStyle,
    val xl: ShadowStyle,
) {
    companion object {
        fun default() = ShadowTokens(
            none = ShadowStyle(Color.TRANSPARENT, 0f, 0f, 0f, 0f),
            sm = ShadowStyle(Color.BLACK, 0.04f, 0f, 1f, 2f),
            md = ShadowStyle(Color.BLACK, 0.08f, 0f, 2f, 4f),
            lg = ShadowStyle(Color.BLACK, 0.12f, 0f, 4f, 8f),
            xl = ShadowStyle(Color.BLACK, 0.16f, 0f, 8f, 16f),
        )
    }
}

/**
 * Shadow descritor em DP. Elevação efetiva é o radius — Android não tem 1:1 com iOS,
 * componentes mapeiam para `elevation` + tinted background.
 */
data class ShadowStyle(
    val color: Int,
    val opacity: Float,
    val offsetX: Float,
    val offsetY: Float,
    val radius: Float,
) {
    val elevationDp: Float get() = radius
}

data class AnimationTokens(
    val durationFastMs: Long = 150L,
    val durationMediumMs: Long = 250L,
    val durationSlowMs: Long = 350L,
    val durationXSlowMs: Long = 500L,
) {
    val standard: Interpolator = AccelerateDecelerateInterpolator()
    val accelerate: Interpolator = AccelerateInterpolator()
    val decelerate: Interpolator = DecelerateInterpolator()
    val linear: Interpolator = LinearInterpolator()
}
