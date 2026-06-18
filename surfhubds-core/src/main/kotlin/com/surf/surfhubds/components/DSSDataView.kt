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
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSDataView` do iOS — bloco "Internet / 10GB / progress / disponível XGB".
 * Pertence ao [DSSCardPlanRechargeView]. Reusa o drawable arredondado de
 * [DSSValidityView] para o track branco com borda `systemGray4`.
 */
class DSSDataView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    val internetLabel = TextView(context).apply {
        text = AppStrings.brand(context, "data_view_internet", "Internet")
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
    }
    val gigasLabel = TextView(context).apply {
        text = "10GB"
        textSize = 24f
        typeface = DSSFont.bold(context, 24f).typeface
    }
    val progressDataView = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
        max = 100
        progress = 0
        progressDrawable = DSSValidityView.buildRoundedProgressDrawable(context)
        clipToOutline = true
    }
    val availableLabel = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
    }

    init {
        orientation = VERTICAL
        addView(internetLabel)
        addView(
            gigasLabel,
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 8f.dpToPx(context)
            },
        )
        addView(
            progressDataView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8f.dpToPx(context)).apply {
                topMargin = 4f.dpToPx(context)
            },
        )
        addView(
            availableLabel,
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
        internetLabel.setTextColor(txt)
        gigasLabel.setTextColor(txt)
        availableLabel.setTextColor(txt)
    }
}
