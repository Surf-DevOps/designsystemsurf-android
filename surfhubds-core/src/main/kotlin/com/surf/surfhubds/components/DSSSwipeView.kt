package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.ncorti.slidetoact.SlideToActView
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

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

    /** Acesso direto à view da lib pra ajustes finos não cobertos pelas props. */
    val slide: SlideToActView = SlideToActView(context).apply {
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
        // Valores exatos do XML do flachip-android:
        //   app:area_margin="4dp"  app:icon_margin="8dp"
        // Em slidetoact 0.11.0 os campos privados são:
        //   `mActualAreaMargin` (mutável) ← usar este pra area_margin
        //   `mOriginAreaMargin` (final, lido só na inflate via attrs)
        //   `mIconMargin` (final Kotlin val) ← precisa override via reflection
        // O `mAreaMargin` que eu usava antes não existe (a reflection falhava
        // silenciosamente, deixando os defaults da lib).
        // Com slider 44dp: thumb = 44 - 2*4 = 36dp, seta = 36 - 2*8 = 20dp.
        setPrivateDimen("mActualAreaMargin", 4f.dpToPx(context))
        setPrivateDimen("mOriginAreaMargin", 4f.dpToPx(context))
        setPrivateDimen("mIconMargin", 8f.dpToPx(context))
        slide.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                onCompleted?.invoke()
                delegate?.didFinish(this@DSSSwipeView)
            }
        }
        setupThemeObserver()
    }

    private fun setPrivateDimen(fieldName: String, value: Int) {
        val ok = runCatching {
            val field = SlideToActView::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.setInt(slide, value)
            slide.requestLayout()
            slide.invalidate()
        }
        if (ok.isFailure) {
            android.util.Log.w(
                "DSSSwipeView",
                "Falha ao setar $fieldName via reflection: ${ok.exceptionOrNull()?.message}",
            )
        }
    }

    override fun applyTheme(theme: Theme) {
        outerColor = DSSColors.primary()
    }

    /** Reseta o thumb pra posição inicial — equivalente ao `resetStateWithAnimation` do iOS. */
    fun resetState(animated: Boolean) {
        slide.setCompleted(false, animated)
    }
}
