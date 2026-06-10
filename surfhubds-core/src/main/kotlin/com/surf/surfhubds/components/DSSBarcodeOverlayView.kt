package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.surf.surfhubds.util.dpToPxFloat

/**
 * Port do `DSSBarcodeOverlayView` do iOS — overlay para câmera de leitor de código
 * de barras: linha central + 4 cantos arredondados.
 *
 * Pintura em [onDraw]: transparente por baixo, brancos no traço.
 */
class DSSBarcodeOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val lineWidth = 4f.dpToPxFloat(context)
        val cornerRadius = 12f.dpToPxFloat(context)
        val cornerLength = 30f.dpToPxFloat(context)
        val margin = (-10f).dpToPxFloat(context)
        val scanWidth = width.toFloat() - (margin * 2)
        val scanHeight = 150f.dpToPxFloat(context)
        val rect = RectF(
            margin,
            (height.toFloat() - scanHeight) / 2f,
            margin + scanWidth,
            (height.toFloat() - scanHeight) / 2f + scanHeight,
        )

        strokePaint.strokeWidth = lineWidth

        // Linha central horizontal — no iOS é desenhada via CGContext com lineCap = .round
        strokePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), strokePaint)

        val path = Path()

        // Canto superior esquerdo
        path.moveTo(rect.left, rect.top + cornerLength - cornerRadius)
        path.arcTo(
            RectF(
                rect.left,
                rect.top,
                rect.left + cornerRadius * 2,
                rect.top + cornerRadius * 2,
            ),
            180f, 90f, false,
        )
        path.lineTo(rect.left + cornerLength, rect.top)

        // Canto inferior esquerdo
        path.moveTo(rect.left, rect.bottom - cornerLength + cornerRadius)
        path.arcTo(
            RectF(
                rect.left,
                rect.bottom - cornerRadius * 2,
                rect.left + cornerRadius * 2,
                rect.bottom,
            ),
            180f, -90f, false,
        )
        path.lineTo(rect.left + cornerLength, rect.bottom)

        // Canto superior direito
        path.moveTo(rect.right, rect.top + cornerLength - cornerRadius)
        path.arcTo(
            RectF(
                rect.right - cornerRadius * 2,
                rect.top,
                rect.right,
                rect.top + cornerRadius * 2,
            ),
            0f, -90f, false,
        )
        path.lineTo(rect.right - cornerLength, rect.top)

        // Canto inferior direito
        path.moveTo(rect.right, rect.bottom - cornerLength + cornerRadius)
        path.arcTo(
            RectF(
                rect.right - cornerRadius * 2,
                rect.bottom - cornerRadius * 2,
                rect.right,
                rect.bottom,
            ),
            0f, 90f, false,
        )
        path.lineTo(rect.right - cornerLength, rect.bottom)

        // Os cantos no iOS usam um CAShapeLayer sem definir `lineCap`, que tem
        // como padrão `kCALineCapButt` (extremidades retas, não arredondadas).
        strokePaint.strokeCap = Paint.Cap.BUTT
        canvas.drawPath(path, strokePaint)
    }
}
