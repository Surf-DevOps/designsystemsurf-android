package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSValidityView` do iOS — bloco "Vence em / X dias / progress / válido até".
 * Pertence ao [DSSCardPlanRechargeView].
 */
class DSSValidityView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    val expiresOnLabel = TextView(context).apply {
        text = "Vence em"
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
        progress = 10
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
}
