package com.surf.surfhubds.theme

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Brilho radial de duas cores: [centerColor] concentrado no ponto ([centerXFraction],
 * [centerYFraction]) — frações 0–1 da largura/altura do bounds — dissipando pra [edgeColor]
 * num raio de [radiusFraction] * altura. Além do raio fica [edgeColor] sólido (CLAMP).
 *
 * É o formato "glow" dos gradientes de marca tipo "Luz azul, do elétrico ao navy" (guia da
 * Chip da Saúde, pág. 05) — um brilho concentrado num canto que se dissipa, não um degradê
 * linear de 2 pontos.
 */
class RadialGradientDrawable(
    private val centerColor: Int,
    private val edgeColor: Int,
    private val centerXFraction: Float,
    private val centerYFraction: Float,
    private val radiusFraction: Float,
) : Drawable() {

    private val paint = Paint()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        val radius = (h * radiusFraction).coerceAtLeast(1f)
        paint.shader = RadialGradient(
            w * centerXFraction,
            h * centerYFraction,
            radius,
            centerColor,
            edgeColor,
            Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            paint,
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
