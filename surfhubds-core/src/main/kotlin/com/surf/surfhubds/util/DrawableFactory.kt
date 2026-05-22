package com.surf.surfhubds.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import androidx.annotation.ColorInt

/**
 * Cria backgrounds com canto arredondado, borda opcional e ripple por cima.
 * Equivalente prático ao `cornerRadius` + `borderWidth` + `backgroundColor` do iOS.
 */
object DrawableFactory {

    fun rounded(
        context: Context,
        @ColorInt backgroundColor: Int,
        cornerRadiusDp: Float,
        @ColorInt strokeColor: Int? = null,
        strokeWidthDp: Float = 0f,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(backgroundColor)
        cornerRadius = cornerRadiusDp.dpToPxFloat(context)
        if (strokeColor != null && strokeWidthDp > 0f) {
            setStroke(strokeWidthDp.dpToPx(context), strokeColor)
        }
    }

    fun rippleOver(
        base: Drawable,
        @ColorInt rippleColor: Int,
    ): RippleDrawable = RippleDrawable(ColorStateList.valueOf(rippleColor), base, null)
}
