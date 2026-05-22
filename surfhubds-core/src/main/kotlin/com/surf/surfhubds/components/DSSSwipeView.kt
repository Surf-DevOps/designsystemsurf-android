package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import kotlin.math.max
import kotlin.math.min

/**
 * Port do `DSSSwipeView` do iOS — "slide to confirm". Usuário arrasta um thumb
 * circular até o final da trilha; ao soltar no fim dispara [onCompleted] / [delegate].
 */
class DSSSwipeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    fun interface Delegate { fun didFinish(sender: DSSSwipeView) }

    val textLabel = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
        gravity = Gravity.CENTER
    }
    val sliderTextLabel = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
        gravity = Gravity.CENTER
        visibility = INVISIBLE
    }
    val sliderHolderView = FrameLayout(context)
    val draggedView = FrameLayout(context)
    val thumbnailImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER
    }

    var delegate: Delegate? = null
    var onCompleted: (() -> Unit)? = null

    var sliderCornerRadiusDp: Float = 22f
        set(value) { field = value; refresh() }

    @ColorInt var sliderBackgroundColor: Int = Color.argb(25, 26, 156, 214)
        set(value) { field = value; refresh() }

    @ColorInt var slidingColor: Int = Color.argb(178, 25, 155, 215)
        set(value) { field = value; refresh() }

    @ColorInt var thumbnailColor: Int = Color.WHITE
        set(value) { field = value; thumbnailImageView.setBackgroundColor(value) }

    @ColorInt var thumbnailTintColor: Int = Color.BLACK
        set(value) { field = value; thumbnailImageView.setColorFilter(value) }

    @ColorInt var textColor: Int = Color.argb(178, 25, 155, 215)
        set(value) { field = value; textLabel.setTextColor(value) }

    var labelText: CharSequence = "Swipe to open"
        set(value) {
            field = value
            textLabel.text = value
            sliderTextLabel.text = value
        }

    private var thumbX: Float = 0f
    private var isFinished: Boolean = false
    private val thumbSizeDp = 37f

    init {
        addView(sliderHolderView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        sliderHolderView.addView(
            textLabel,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        addView(
            draggedView,
            LayoutParams(0, LayoutParams.MATCH_PARENT),
        )
        draggedView.addView(
            sliderTextLabel,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        addView(
            thumbnailImageView,
            LayoutParams(thumbSizeDp.dpToPx(context), thumbSizeDp.dpToPx(context)).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            },
        )
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        sliderHolderView.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = sliderBackgroundColor,
            cornerRadiusDp = sliderCornerRadiusDp,
        )
        draggedView.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = slidingColor,
            cornerRadiusDp = sliderCornerRadiusDp,
        )
        thumbnailImageView.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = thumbnailColor,
            cornerRadiusDp = sliderCornerRadiusDp,
        )
        thumbnailImageView.setColorFilter(thumbnailTintColor)
        textLabel.setTextColor(textColor)
        sliderTextLabel.setTextColor(sliderBackgroundColor)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyThumbPosition()
    }

    private fun applyThumbPosition() {
        val w = thumbnailImageView.width
        if (w == 0) return
        thumbnailImageView.translationX = thumbX
        val draggedLp = draggedView.layoutParams
        draggedLp.width = (thumbX + w).toInt()
        draggedView.layoutParams = draggedLp
    }

    @Volatile private var dragStartX: Float = 0f
    @Volatile private var thumbStartX: Float = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return ev.action == MotionEvent.ACTION_DOWN && thumbnailImageView.let { tn ->
            val x = ev.x; val y = ev.y
            x in tn.translationX..(tn.translationX + tn.width) &&
                y in tn.top.toFloat()..tn.bottom.toFloat()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isFinished) return false
        val maxX = (width - thumbnailImageView.width).toFloat()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.rawX
                thumbStartX = thumbX
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - dragStartX
                thumbX = min(max(0f, thumbStartX + dx), maxX)
                textLabel.alpha = if (maxX <= 0) 1f else (maxX - thumbX) / maxX
                applyThumbPosition()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (thumbX >= maxX - 1f) {
                    isFinished = true
                    textLabel.alpha = 0f
                    thumbX = maxX
                    applyThumbPosition()
                    onCompleted?.invoke()
                    delegate?.didFinish(this)
                } else {
                    animate(thumbX, 0f)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun animate(from: Float, to: Float) {
        val animator = android.animation.ValueAnimator.ofFloat(from, to)
        animator.duration = 200
        animator.addUpdateListener {
            thumbX = it.animatedValue as Float
            textLabel.alpha = 1f
            applyThumbPosition()
        }
        animator.start()
    }

    /** Reseta o thumb pra posição inicial. */
    fun resetState(animated: Boolean) {
        isFinished = false
        if (animated) animate(thumbX, 0f) else {
            thumbX = 0f
            textLabel.alpha = 1f
            applyThumbPosition()
        }
    }
}
