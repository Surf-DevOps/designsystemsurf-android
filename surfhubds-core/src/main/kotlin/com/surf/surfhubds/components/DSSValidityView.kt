package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSValidityView` do iOS — bloco "Vence em / X dias / progress / válido até".
 * Pertence ao [DSSCardPlanRechargeView]. Espelha 1:1 o componente Swift:
 *  - `progressDaysView` com track `.white`, `cornerRadius = 4`, borda `systemGray4`,
 *    `clipsToBounds = true` e altura 8.
 */
class DSSValidityView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    val expiresOnLabel = TextView(context).apply {
        text = AppStrings.brand(context, "validity_view_expires_in", "Vence em")
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
    }
    val daysLabel = TextView(context).apply {
        text = "25 dias"
        textSize = 24f
        typeface = DSSFont.bold(context, 24f).typeface
    }
    val progressDaysView = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 100
        progress = 0
        progressDrawable = buildRoundedProgressDrawable(context)
        clipToOutline = true
    }
    val validUntilLabel = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
    }

    init {
        orientation = VERTICAL
        addView(expiresOnLabel)
        addView(
            daysLabel,
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8f.dpToPx(context)
            },
        )
        addView(
            progressDaysView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8f.dpToPx(context)).apply {
                topMargin = 4f.dpToPx(context)
            },
        )
        addView(
            validUntilLabel,
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4f.dpToPx(context)
            },
        )
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val txt = DSSColors.textPrimary()
        expiresOnLabel.setTextColor(txt)
        daysLabel.setTextColor(txt)
        validUntilLabel.setTextColor(txt)
    }

    companion object {
        /**
         * Drawable composto espelhando o `UIProgressView` do iOS:
         *  - track branca com `cornerRadius=4` e borda 1dp `systemGray4`
         *  - barra de progresso (sem cor — tinge via `progressTintList` no caller)
         */
        internal fun buildRoundedProgressDrawable(context: Context): LayerDrawable {
            val radius = 4f.dpToPx(context).toFloat()
            val border = 1f.dpToPx(context)
            val track = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(Color.WHITE)
                setStroke(border, Color.parseColor("#D1D1D6")) // systemGray4 light
            }
            val progressShape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(DSSColors.primary())
            }
            val progressClip = ClipDrawable(progressShape, Gravity.START, ClipDrawable.HORIZONTAL)
            val layer = LayerDrawable(arrayOf(track, progressClip))
            layer.setId(0, android.R.id.background)
            layer.setId(1, android.R.id.progress)
            return layer
        }
    }
}
