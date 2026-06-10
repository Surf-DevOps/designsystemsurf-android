package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Port do `DSSConsumptionChartFla` do iOS (variante FLA do FlaChip) — gauge igual ao
 * [DSSConsumptionChart] mas com layout interno "title + usado / total" e suporte ao
 * modo invertido (usado = total - used).
 */
class DSSConsumptionChartFla @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class TypeChart { GB, MIN, SMS }

    var used: Double = 10.0
        set(value) { field = value; arcView.invalidate(); updateLabels() }
    var total: Double = 20.0
        set(value) { field = value; arcView.invalidate(); updateLabels() }
    var type: TypeChart = TypeChart.GB
        set(value) { field = value; arcView.invalidate(); updateLabels() }
    var isInverted: Boolean = false
        set(value) { field = value; arcView.invalidate(); updateLabels() }
    var progressColor: Int = DSSColors.primary()
        set(value) { field = value; arcView.invalidate() }
    // iOS: UIColor.lightGray.withAlphaComponent(0.3) -> lightGray == rgb(170,170,170)
    var trackColor: Int = Color.argb(77, 170, 170, 170)
        set(value) { field = value; arcView.invalidate() }
    var usedTextColor: Int = Color.BLACK
        set(value) {
            field = value
            titleLabel.setTextColor(value)
            usedLabel.setTextColor(value)
        }
    // iOS: default .darkGray. totalLabel fica oculto no iOS; mantido para paridade de API.
    var totalTextColor: Int = Color.rgb(85, 85, 85)
    var chartAction: (() -> Unit)? = null

    private val arcView = ArcView(context)

    private val titleLabel = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 20f
        typeface = DSSFont.light(context, 20f).typeface
    }
    private val usedLabel = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 20f
        typeface = DSSFont.light(context, 20f).typeface
    }

    init {
        addView(arcView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val stack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        stack.addView(titleLabel)
        stack.addView(
            usedLabel,
            LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10f.dpToPx(context)
            },
        )
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            topMargin = -30f.dpToPx(context)
        }
        addView(stack, lp)
        // iOS: titleLabel.textColor = usedTextColor ; usedLabel.textColor = usedTextColor
        titleLabel.setTextColor(usedTextColor)
        usedLabel.setTextColor(usedTextColor)
        setOnClickListener { chartAction?.invoke() }
        updateLabels()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { arcView.invalidate() }

    private fun updateLabels() {
        when (type) {
            TypeChart.GB -> {
                titleLabel.text = "Internet"
                // iOS: used/1000 e total/1000 como Double, formatados com %.0f (arredonda, nao trunca).
                val usedText = String.format(Locale.US, "%.0f", used / 1000.0)
                val totalText = String.format(Locale.US, "%.0f", total / 1000.0)
                usedLabel.text = makeColoredAfterSlash("${usedText}GB / ${totalText}GB", DSSColors.primary())
            }
            TypeChart.SMS -> {
                titleLabel.text = "SMS"
                val usedValue = if (isInverted) max(0.0, total - used) else used
                val usedText = String.format(Locale.US, "%.0f", usedValue)
                val totalText = String.format(Locale.US, "%.0f", total)
                usedLabel.text = makeColoredAfterSlash("$usedText / $totalText", DSSColors.primary())
            }
            TypeChart.MIN -> {
                titleLabel.text = "Minutos"
                if (total > 900) {
                    usedLabel.text = "Ilimitado"
                } else {
                    val usedValue = if (isInverted) max(0.0, total - used) else used
                    val usedText = String.format(Locale.US, "%.0f", usedValue)
                    val totalText = String.format(Locale.US, "%.0f", total)
                    usedLabel.text = makeColoredAfterSlash("${usedText}min / ${totalText}min", DSSColors.primary())
                }
            }
        }
    }

    private fun makeColoredAfterSlash(text: String, color: Int): CharSequence {
        val idx = text.indexOf('/')
        if (idx < 0) return text
        return SpannableStringBuilder(text).apply {
            setSpan(ForegroundColorSpan(color), idx + 1, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private inner class ArcView(context: Context) : View(context) {
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 18f.dpToPx(context).toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 18f.dpToPx(context).toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        private val pointerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = min(width, height) / 2.2f
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

            val startDeg = 135f
            val sweepDeg = 270f

            trackPaint.color = trackColor
            canvas.drawArc(rect, startDeg, sweepDeg, false, trackPaint)

            // iOS: percent = used / max(total, 1) (SEM clamp superior);
            //      effectivePercent = isInverted ? max(0, 1 - percent) : percent
            val effectivePercent = if (usedLabel.text == "Ilimitado") {
                0f
            } else {
                val percent = (used / max(total, 1.0)).toFloat()
                if (isInverted) max(0f, 1f - percent) else percent
            }

            // iOS: progressLayer.strokeEnd e clampado em [0,1] pelo CAShapeLayer -> arco limitado.
            val arcPercent = effectivePercent.coerceIn(0f, 1f)
            progressPaint.color = progressColor
            canvas.drawArc(rect, startDeg, sweepDeg * arcPercent, false, progressPaint)

            // iOS: o pointer usa o effectivePercent BRUTO (sem clamp) no calculo do angulo.
            val rad = Math.toRadians((startDeg + sweepDeg * effectivePercent).toDouble())
            val px = cx + radius * cos(rad).toFloat()
            val py = cy + radius * sin(rad).toFloat()
            pointerFill.color = Color.WHITE
            canvas.drawCircle(px, py, 8f.dpToPx(context).toFloat(), pointerFill)
        }
    }
}
