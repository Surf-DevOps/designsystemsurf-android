package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Port do `DSSConsumptionChart` do iOS — gauge em arco (270 graus) com label central
 * "total" + sub-label "disponível / utilizado".
 */
class DSSConsumptionChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class TypeChart { GB, MIN, SMS }

    var used: Int = 10
        set(value) { field = value; arcView.invalidate(); updateLabels() }
    var total: Int = 20
        set(value) { field = value; arcView.invalidate(); updateLabels() }
    var type: TypeChart = TypeChart.GB
        set(value) { field = value; arcView.invalidate(); updateLabels() }
    var isInverted: Boolean = false
        set(value) { field = value; arcView.invalidate() }

    var progressColor: Int = DSSColors.primary()
        set(value) { field = value; arcView.invalidate() }
    var trackColor: Int = Color.argb(77, 200, 200, 200)
        set(value) { field = value; arcView.invalidate() }
    var usedFontSizeSp: Float = 26f
        set(value) { field = value; totalLabel.textSize = value }
    var totalFontSizeSp: Float = 14f
        set(value) { field = value; usedLabel.textSize = value }
    var usedTextColor: Int = Color.BLACK
        set(value) { field = value; totalLabel.setTextColor(value) }
    var totalTextColor: Int = Color.DKGRAY
        set(value) { field = value; usedLabel.setTextColor(value) }

    var chartAction: (() -> Unit)? = null

    private val arcView = ArcView(context)
    private val totalLabel = TextView(context).apply {
        textSize = 26f
        gravity = Gravity.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val usedLabel = TextView(context).apply {
        textSize = 14f
        gravity = Gravity.CENTER
        maxLines = 2
    }

    init {
        addView(arcView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val centerStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        centerStack.addView(totalLabel)
        centerStack.addView(
            usedLabel,
            LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4f.dpToPx(context)
            },
        )
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
        addView(centerStack, lp)
        setOnClickListener { chartAction?.invoke() }
        updateLabels()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { arcView.invalidate() }

    private fun updateLabels() {
        when (type) {
            TypeChart.GB -> {
                totalLabel.text = "${formatMbToGb(total)}GB"
                usedLabel.text = "disponível\nde ${formatMbToGb(used)}GB"
            }
            TypeChart.SMS -> {
                totalLabel.text = "${total}SMS"
                usedLabel.text = "Utilizado: ${total - used}SMS"
            }
            TypeChart.MIN -> {
                totalLabel.text = if (total > 900) "Ilimitado" else "${used}min"
                usedLabel.text = "Utilizado: ${total - used}min"
            }
        }
    }

    private fun formatMbToGb(mb: Int): String {
        val gb = mb / 1000.0
        val r = (gb * 10).roundToInt() / 10.0
        return if (r % 1.0 == 0.0) r.toInt().toString() else r.toString()
    }

    /** Sub-view que desenha o arco + pointer. */
    private inner class ArcView(context: Context) : View(context) {
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 16f.dpToPx(context).toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 16f.dpToPx(context).toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        private val pointerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val pointerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f.dpToPx(context).toFloat()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = min(width, height) / 2.2f
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

            // angles: iOS uses 3pi/4 .. pi/4 going clockwise (sweep 270 degrees)
            val startDeg = 135f
            val sweepDeg = 270f

            trackPaint.color = trackColor
            canvas.drawArc(rect, startDeg, sweepDeg, false, trackPaint)

            val percent = when (type) {
                TypeChart.GB -> ((used - total).toFloat() / max(used, 1))
                else -> ((total - used).toFloat() / max(used, 1))
            }.coerceIn(0f, 1f)

            progressPaint.color = progressColor
            canvas.drawArc(rect, startDeg, sweepDeg * percent, false, progressPaint)

            // Pointer
            val rad = Math.toRadians((startDeg + sweepDeg * percent).toDouble())
            val px = cx + radius * cos(rad).toFloat()
            val py = cy + radius * sin(rad).toFloat()
            pointerFill.color = Color.WHITE
            pointerStroke.color = progressColor
            val pr = 6.5f.dpToPx(context).toFloat()
            canvas.drawCircle(px, py, pr, pointerFill)
            canvas.drawCircle(px, py, pr, pointerStroke)
        }
    }
}
