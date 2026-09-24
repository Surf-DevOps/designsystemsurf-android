package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Quadro da câmera do leitor de QR Code (Figma): fundo escuro com cantos levemente
 * arredondados, e por cima do preview (filho no XML) a mira — quatro cantoneiras brancas
 * e a linha de leitura em `primaryButton`.
 *
 * O fundo é escuro nos dois temas de propósito: é a área da câmera. Para o preview
 * respeitar os cantos, use o PreviewView em modo COMPATIBLE (TextureView).
 */
class DSSScannerFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    private val inset = 40f.dpToPx(context).toFloat()
    private val arm = 36f.dpToPx(context).toFloat()
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f.dpToPx(context).toFloat()
        color = Color.WHITE
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f.dpToPx(context).toFloat()
    }
    private val path = Path()

    init {
        background = DrawableFactory.rounded(context, CAMERA_BACKGROUND, 4f)
        clipToOutline = true
        setWillNotDraw(false)
        refresh()
        setupThemeObserver()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val l = inset; val t = inset; val r = width - inset; val b = height - inset
        if (r <= l || b <= t) return
        val half = cornerPaint.strokeWidth / 2f
        path.reset()
        // Cantoneiras (a meia espessura para dentro, para o traço não vazar da mira).
        path.moveTo(l + half, t + arm); path.lineTo(l + half, t + half); path.lineTo(l + arm, t + half)
        path.moveTo(r - arm, t + half); path.lineTo(r - half, t + half); path.lineTo(r - half, t + arm)
        path.moveTo(r - half, b - arm); path.lineTo(r - half, b - half); path.lineTo(r - arm, b - half)
        path.moveTo(l + arm, b - half); path.lineTo(l + half, b - half); path.lineTo(l + half, b - arm)
        canvas.drawPath(path, cornerPaint)
        // Linha de leitura a ~30% da altura da mira (posição do Figma).
        val y = t + (b - t) * SCAN_LINE_POSITION
        canvas.drawLine(l, y, r, y, linePaint)
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        linePaint.color = DSSColors.primaryButton()
        invalidate()
    }

    private companion object {
        val CAMERA_BACKGROUND = Color.parseColor("#15191E")
        const val SCAN_LINE_POSITION = 0.3f
    }
}
