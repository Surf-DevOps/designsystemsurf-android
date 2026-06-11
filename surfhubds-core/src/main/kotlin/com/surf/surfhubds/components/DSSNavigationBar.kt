package com.surf.surfhubds.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorInt
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSNavigationBar` do iOS — title + back/action buttons + progress bar opcional.
 */
class DSSNavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class TitleAlignment { CENTER, LEFT }

    private val titleLabel = TextView(context)
    private val leftButton = ImageButton(context)
    private val rightButton = ImageButton(context)
    private val progressBar = DSSProgressBarView(context)

    var leftAction: (() -> Unit)? = null
    var rightAction: (() -> Unit)? = null

    @ColorInt private var titleColorOverride: Int? = null
    @ColorInt private var leftTintOverride: Int? = null
    @ColorInt private var rightTintOverride: Int? = null
    @ColorInt private var backgroundColorOverride: Int? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)

        // iOS: font padrão = DSSFont.light(18)
        titleLabel.textSize = 18f
        titleLabel.typeface = DSSFont.light(context, 18f).typeface
        titleLabel.gravity = Gravity.CENTER
        addView(titleLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })

        leftButton.background = null
        // iOS: contentEdgeInsets = .zero / imageEdgeInsets = .zero
        leftButton.setPadding(0, 0, 0, 0)
        leftButton.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        leftButton.setOnClickListener { leftAction?.invoke() }
        leftButton.visibility = View.GONE
        addView(leftButton, LayoutParams(32f.dpToPx(context), 32f.dpToPx(context)).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            leftMargin = 16f.dpToPx(context)
        })

        rightButton.background = null
        // iOS: contentEdgeInsets = .zero / imageEdgeInsets = .zero
        rightButton.setPadding(0, 0, 0, 0)
        rightButton.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        rightButton.setOnClickListener { rightAction?.invoke() }
        rightButton.visibility = View.GONE
        addView(rightButton, LayoutParams(7f.dpToPx(context), 24f.dpToPx(context)).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            rightMargin = 35f.dpToPx(context)
        })

        progressBar.visibility = View.GONE
        addView(progressBar, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 4f.dpToPx(context)).apply {
            gravity = Gravity.BOTTOM
            topMargin = 16f.dpToPx(context)
        })

        refresh()
        setupThemeObserver()
    }

    fun configure(
        title: CharSequence,
        leftIcon: Drawable? = null,
        rightIcon: Drawable? = null,
        alignment: TitleAlignment = TitleAlignment.CENTER,
        showsProgressBar: Boolean = false,
        progressTotalSteps: Int = 1,
        progressCurrentStep: Int = 1,
        // iOS init: parâmetros adicionais (todos opcionais p/ não quebrar API existente)
        font: Typeface? = null,
        widthRightButton: Float = 7f,
        heightRightButton: Float = 24f,
        @ColorInt leftTintColor: Int? = null,
        @ColorInt rightTintColor: Int? = null,
        @ColorInt titleColor: Int? = null,
        @ColorInt backgroundColorOverride: Int? = null,
    ) {
        titleLabel.text = title
        // iOS: font padrão = DSSFont.light(18); só sobrescreve se fornecida
        font?.let { titleLabel.typeface = it }
        val titleLp = titleLabel.layoutParams as LayoutParams
        if (alignment == TitleAlignment.LEFT) {
            titleLp.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            titleLp.leftMargin = 48f.dpToPx(context)
        } else {
            titleLp.gravity = Gravity.CENTER
            titleLp.leftMargin = 0
        }
        titleLabel.layoutParams = titleLp

        leftButton.visibility = if (leftIcon != null) View.VISIBLE else View.GONE
        leftIcon?.let {
            leftButton.setImageDrawable(it)
            leftButton.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }

        rightButton.visibility = if (rightIcon != null) View.VISIBLE else View.GONE
        rightIcon?.let {
            rightButton.setImageDrawable(it)
            rightButton.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        // iOS: tamanho do botão direito é configurável (default 7x24)
        (rightButton.layoutParams as? LayoutParams)?.let { rlp ->
            rlp.width = widthRightButton.dpToPx(context)
            rlp.height = heightRightButton.dpToPx(context)
            rightButton.layoutParams = rlp
        }

        // iOS: cores configuráveis na init
        leftTintColor?.let { leftTintOverride = it }
        rightTintColor?.let { rightTintOverride = it }
        titleColor?.let { titleColorOverride = it }
        backgroundColorOverride?.let { this.backgroundColorOverride = it }

        progressBar.visibility = if (showsProgressBar) View.VISIBLE else View.GONE
        if (showsProgressBar) progressBar.configure(progressTotalSteps, progressCurrentStep, animated = false)
        refresh()
    }

    @Deprecated("Use setRightButtonHidden(_:) instead", ReplaceWith("setRightButtonHidden(isHidden)"))
    fun plusButton(isHidden: Boolean) {
        rightButton.visibility = if (isHidden) View.GONE else View.VISIBLE
    }

    fun setRightButtonHidden(hidden: Boolean) {
        rightButton.visibility = if (hidden) View.GONE else View.VISIBLE
    }

    fun setProgressBarHidden(hidden: Boolean) {
        progressBar.visibility = if (hidden) View.GONE else View.VISIBLE
    }

    fun configureProgress(totalSteps: Int, currentStep: Int, animated: Boolean = true) {
        progressBar.visibility = View.VISIBLE
        progressBar.configure(totalSteps, currentStep, animated)
    }

    fun setBackgroundColorOverride(@ColorInt color: Int?) {
        backgroundColorOverride = color
        setBackgroundColor(color ?: Color.TRANSPARENT)
    }

    fun setTitleColor(@ColorInt color: Int?) {
        titleColorOverride = color
        titleLabel.setTextColor(color ?: DSSColors.textPrimary())
    }

    fun setLeftTintColor(@ColorInt color: Int?) {
        leftTintOverride = color
        // iOS: só aplica tint se o botão tiver imagem (currentImage != nil)
        if (leftButton.drawable != null) {
            leftButton.setColorFilter(color ?: DSSColors.textPrimary(), PorterDuff.Mode.SRC_IN)
        }
    }

    fun setRightTintColor(@ColorInt color: Int?) {
        rightTintOverride = color
        // iOS: só aplica tint se o botão tiver imagem (currentImage != nil)
        if (rightButton.drawable != null) {
            rightButton.setColorFilter(color ?: DSSColors.textPrimary(), PorterDuff.Mode.SRC_IN)
        }
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        backgroundColorOverride?.let { setBackgroundColor(it) }
        titleLabel.setTextColor(titleColorOverride ?: DSSColors.textPrimary())
        leftButton.setColorFilter(leftTintOverride ?: DSSColors.textPrimary(), PorterDuff.Mode.SRC_IN)
        rightButton.setColorFilter(rightTintOverride ?: DSSColors.textPrimary(), PorterDuff.Mode.SRC_IN)
    }
}

class DSSProgressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), ThemeAware {

    private val backgroundPaint = android.graphics.Paint().apply { color = Color.TRANSPARENT }
    private val progressPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = DSSColors.primary()
    }
    private val backgroundRect = android.graphics.RectF()
    private val progressRect = android.graphics.RectF()

    private var totalSteps: Int = 1
    private var currentStep: Int = 1
    private var animatedFraction: Float = 1f

    init { setupThemeObserver() }

    fun configure(totalSteps: Int, currentStep: Int, animated: Boolean = true) {
        this.totalSteps = totalSteps.coerceAtLeast(1)
        this.currentStep = currentStep.coerceIn(1, this.totalSteps)
        // iOS: guard totalSteps > 1 else { return bounds.width } -> 1 passo = barra cheia
        val target = if (this.totalSteps > 1) {
            this.currentStep.toFloat() / this.totalSteps.toFloat()
        } else {
            1f
        }
        if (animated) {
            ValueAnimator.ofFloat(animatedFraction, target).apply {
                duration = 300
                addUpdateListener {
                    this@DSSProgressBarView.animatedFraction = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            animatedFraction = target
            invalidate()
        }
    }

    override fun applyTheme(theme: Theme) {
        progressPaint.color = DSSColors.primary()
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val h = height.toFloat()
        val y = (h - 4f * resources.displayMetrics.density) / 2f
        val barHeight = 4f * resources.displayMetrics.density

        backgroundRect.set(0f, y, width.toFloat(), y + barHeight)
        canvas.drawRoundRect(backgroundRect, barHeight / 2f, barHeight / 2f, backgroundPaint)

        progressRect.set(0f, y, width * animatedFraction, y + barHeight)
        canvas.drawRoundRect(progressRect, barHeight / 2f, barHeight / 2f, progressPaint)
    }
}
