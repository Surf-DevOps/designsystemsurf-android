package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Anel de progresso (Figma "Resgate indisponível"): trilho `divider` e arco
 * `primaryButton`, começando no topo em sentido horário. [progress] vai de 0 a 100.
 * O conteúdo do centro (ícone) fica a cargo do layout.
 */
class DSSProgressRing @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), ThemeAware {

    var progress: Int = 0
        set(value) { field = value.coerceIn(0, 100); invalidate() }

    private val stroke = 10f.dpToPx(context).toFloat()
    private val oval = RectF()
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
    }

    init {
        refresh()
        setupThemeObserver()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val half = stroke / 2f
        oval.set(half, half, width - half, height - half)
        canvas.drawArc(oval, 0f, 360f, false, trackPaint)
        if (progress > 0) canvas.drawArc(oval, -90f, 360f * progress / 100f, false, progressPaint)
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        trackPaint.color = DSSColors.divider()
        progressPaint.color = DSSColors.primaryButton()
        invalidate()
    }
}
