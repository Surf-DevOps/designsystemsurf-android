package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.ncorti.slidetoact.SlideToActView
import com.surf.surfhubds.R
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver

/**
 * Port do `DSSSwipeView` do iOS — "slide to confirm". Internamente embrulha
 * `com.ncorti.slidetoact.SlideToActView` (mesma lib usada no flachip-android).
 *
 * O usuário arrasta o thumb circular até o final da trilha; ao soltar no fim
 * dispara [onCompleted] / [delegate].
 */
class DSSSwipeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    fun interface Delegate { fun didFinish(sender: DSSSwipeView) }

    /**
     * Acesso direto à view da lib pra ajustes finos não cobertos pelas props.
     *
     * `area_margin` / `icon_margin` / `text_size` não têm setter público na lib —
     * só são lidos do `defStyleAttr`/`AttributeSet` no construtor. Por isso o
     * `SlideToActView` é criado com um [ContextThemeWrapper] que aponta
     * `dssSwipeSliderStyle` → `@style/DSS.SwipeSlider`, e esse attr é passado como
     * `defStyleAttr`. A versão antiga setava os campos privados via reflection, o
     * que quebrava no build de release (R8 renomeia os campos) e fazia a seta do
     * thumb sumir. Ver [R.style.DSS_SwipeSlider].
     */
    val slide: SlideToActView = SlideToActView(
        ContextThemeWrapper(context, R.style.ThemeOverlay_DSS_SwipeSlider),
        null,
        R.attr.dssSwipeSliderStyle,
    ).apply {
        animDuration = 200
        isAnimateCompletion = false
    }

    var delegate: Delegate? = null
    var onCompleted: (() -> Unit)? = null

    var labelText: CharSequence = "Swipe to open"
        set(value) {
            field = value
            slide.text = value
        }

    @ColorInt var outerColor: Int = DSSColors.primary()
        set(value) { field = value; slide.outerColor = value }

    @ColorInt var innerColor: Int = Color.WHITE
        set(value) { field = value; slide.innerColor = value }

    @ColorInt var iconColor: Int = Color.BLACK
        set(value) { field = value; slide.iconColor = value }

    @ColorInt var labelTextColor: Int = Color.WHITE
        set(value) { field = value; slide.textColor = value }

    init {
        addView(
            slide,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        slide.outerColor = outerColor
        slide.innerColor = innerColor
        slide.iconColor = iconColor
        slide.textColor = labelTextColor
        slide.text = labelText
        // area_margin (4dp), icon_margin (8dp) e text_size (16sp) vêm do style
        // aplicado via defStyleAttr — ver KDoc de `slide`.
        slide.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                onCompleted?.invoke()
                delegate?.didFinish(this@DSSSwipeView)
            }
        }
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) {
        outerColor = DSSColors.primary()
    }

    /** Reseta o thumb pra posição inicial — equivalente ao `resetStateWithAnimation` do iOS. */
    fun resetState(animated: Boolean) {
        slide.setCompleted(false, animated)
    }
}
