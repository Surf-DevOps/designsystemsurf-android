package com.surf.surfhubds.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dpToPxFloat
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.styleText

/**
 * Linha de abas em pílula (segmented): trilho arredondado em `backgroundSecondary` com abas de
 * largura igual; a selecionada fica numa pílula `primary` com texto em contraste, que desliza
 * até ela; as demais em `textPrimary`. Ex.: "Administrador | Integrante" nos grupos.
 *
 * A altura vem do layout (48dp recomendado). [onSelectionChanged] só dispara por toque.
 */
class DSSTabRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    var onSelectionChanged: ((Int) -> Unit)? = null

    var selectedIndex: Int = 0
        private set

    private val tabs = mutableListOf<TextView>()
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorRect = RectF()
    private val trackRect = RectF()
    private val trackPadding = 4f.dpToPxFloat(context)

    /** Posição do indicador em "abas" (0f = primeira); anima entre seleções. */
    private var indicatorPosition = 0f
    private var animator: ValueAnimator? = null

    init {
        orientation = HORIZONTAL
        setWillNotDraw(false)
        val pad = trackPadding.toInt()
        setPadding(pad, pad, pad, pad)
        refresh()
        setupThemeObserver()
    }

    fun configure(titles: List<String>, selectedIndex: Int = 0) {
        removeAllViews()
        tabs.clear()
        titles.forEachIndexed { index, title ->
            val tab = dssText(context, DSSWeight.MEDIUM, 15f, DSSColors.textSecondary(), maxLines = 1).apply {
                text = title
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setOnClickListener { select(index, fromUser = true) }
            }
            tabs += tab
            addView(tab, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
        this.selectedIndex = selectedIndex.coerceIn(0, (titles.size - 1).coerceAtLeast(0))
        indicatorPosition = this.selectedIndex.toFloat()
        refresh()
    }

    fun select(index: Int, fromUser: Boolean = false) {
        if (index !in tabs.indices) return
        val changed = index != selectedIndex
        selectedIndex = index
        animateIndicator(index.toFloat())
        refresh()
        if (fromUser && changed) onSelectionChanged?.invoke(index)
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun animateIndicator(target: Float) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(indicatorPosition, target).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                indicatorPosition = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun refresh() {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            tab.styleText(
                DSSWeight.MEDIUM,
                16f,
                if (selected) DSSColors.contrastOnPrimary() else DSSColors.textPrimary(),
            )
            tab.isSelected = selected
            tab.background = DrawableFactory.rippleOver(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
                ColorUtils.setAlphaComponent(DSSColors.primary(), 28),
            )
            tab.clipToOutline = true
            tab.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, view.height / 2f)
                }
            }
            tab.contentDescription = tab.text
        }
        indicatorPaint.color = DSSColors.primary()
        trackPaint.color = DSSColors.backgroundSecondary()
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Trilho e pílula são desenhados antes das abas, para o texto ficar por cima.
        trackRect.set(0f, 0f, width.toFloat(), height.toFloat())
        val trackRadius = trackRect.height() / 2
        canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackPaint)
        if (tabs.isNotEmpty()) {
            val inner = width - paddingLeft - paddingRight
            val tabWidth = inner.toFloat() / tabs.size
            val left = paddingLeft + indicatorPosition * tabWidth
            indicatorRect.set(left, paddingTop.toFloat(), left + tabWidth, (height - paddingBottom).toFloat())
            val r = indicatorRect.height() / 2
            canvas.drawRoundRect(indicatorRect, r, r, indicatorPaint)
        }
        super.dispatchDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
