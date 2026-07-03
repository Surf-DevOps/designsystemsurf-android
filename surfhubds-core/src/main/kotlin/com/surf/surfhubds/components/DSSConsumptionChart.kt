package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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
    // iOS default: UIColor.lightGray.withAlphaComponent(0.3) -> lightGray = 170, alpha 0.3 = 77.
    var trackColor: Int = Color.argb(77, 170, 170, 170)
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
        // iOS totalLabel usa numberOfLines default (1).
        maxLines = 1
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val usedLabel = TextView(context).apply {
        textSize = 14f
        gravity = Gravity.CENTER
        maxLines = 2
        // iOS: usedLabel.widthAnchor <= 120
        maxWidth = 120f.dpToPx(context)
    }

    // iOS: arrowButton com systemImage "arrow.up.right" (weight .thin), tint DSSColors.primary, 16x16.
    private val arrowButton = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        setImageDrawable(loadArrowUpRight())
        setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
    }

    init {
        addView(arcView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        val centerStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        // Linha do total: [totalLabel] [4dp] [arrow 16x16] (iOS: arrow trailing do totalLabel, centerY igual).
        val totalRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        totalRow.addView(totalLabel)
        totalRow.addView(
            arrowButton,
            LinearLayout.LayoutParams(16f.dpToPx(context), 16f.dpToPx(context)).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = 4f.dpToPx(context)
            },
        )
        // WRAP_CONTENT explicito: sem isso o LinearLayout vertical usa MATCH_PARENT como
        // default, tornando o totalRow um "match-width child". Isso dispara uma segunda
        // passada de medicao que reconstrange o totalRow a largura do centerStack, e a seta
        // (16dp + 4dp) rouba esse espaco — cortando ~1 caractere do totalLabel ("30.8GB"->"30.8G").
        centerStack.addView(
            totalRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        centerStack.addView(
            usedLabel,
            LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4f.dpToPx(context)
            },
        )
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            // iOS: totalLabel.centerY = centerY - 10 -> bloco de texto deslocado 10dp para cima.
            topMargin = (-10f).dpToPx(context)
        }
        addView(centerStack, lp)
        setOnClickListener { chartAction?.invoke() }
        updateLabels()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) {
        arrowButton.setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
        arcView.invalidate()
    }

    private fun loadArrowUpRight(): Drawable? {
        // Procura `ic_arrow_up_right` nos recursos do app, sem falhar se ausente (espelha "arrow.up.right" do iOS).
        val resId = resources.getIdentifier("ic_arrow_up_right", "drawable", context.packageName)
        return if (resId != 0) AppCompatResources.getDrawable(context, resId) else null
    }

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
        // iOS Utility.formatMBToGBWithDecimal: ceil(gb*10)/10 e sempre 1 casa decimal ("%.1f").
        val gb = mb / 1000.0
        val roundedGb = ceil(gb * 10) / 10
        return String.format(java.util.Locale.US, "%.1f", roundedGb)
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

            // iOS desenha o track com DSSColors.primary (trackColor fica armazenado mas nao e usado p/ o track).
            trackPaint.color = DSSColors.primary()
            canvas.drawArc(rect, startDeg, sweepDeg, false, trackPaint)

            // iOS: remainingPercent (sem clamp) alimenta o angulo do pointer; strokeEnd e clampado a [0,1] pela CA.
            val rawPercent = when (type) {
                TypeChart.GB -> ((used - total).toFloat() / max(used, 1))
                else -> ((total - used).toFloat() / max(used, 1))
            }
            val arcPercent = rawPercent.coerceIn(0f, 1f)

            progressPaint.color = progressColor
            canvas.drawArc(rect, startDeg, sweepDeg * arcPercent, false, progressPaint)

            // Pointer — angulo usa o percentual bruto (espelha iOS: startAngle + remainingPercent * totalAngle).
            val rad = Math.toRadians((startDeg + sweepDeg * rawPercent).toDouble())
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
