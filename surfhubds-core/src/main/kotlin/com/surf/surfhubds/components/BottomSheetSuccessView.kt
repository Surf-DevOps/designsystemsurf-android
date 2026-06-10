package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `BottomSheetSuccesView` do iOS — view de sucesso com icone + título centralizados.
 */
class BottomSheetSuccessView(context: Context) : FrameLayout(context), ThemeAware {

    private val iconImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private val titleLabel = TextView(context).apply {
        gravity = Gravity.CENTER
        typeface = DSSFont.medium(context, 20f).typeface
        textSize = 20f
    }

    private val stackView = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    @ColorInt private var overrideTextColor: Int? = null

    init {
        addView(stackView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            leftMargin = 24f.dpToPx(context)
            rightMargin = 24f.dpToPx(context)
        })

        stackView.addView(iconImageView, LinearLayout.LayoutParams(
            200f.dpToPx(context), 134f.dpToPx(context),
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 30f.dpToPx(context)
        })

        stackView.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { gravity = Gravity.CENTER_HORIZONTAL })

        refresh()
        setupThemeObserver()
    }

    fun configure(image: Drawable?, text: String, @ColorInt textColor: Int? = null) {
        iconImageView.setImageDrawable(image)
        titleLabel.text = text
        overrideTextColor = textColor
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        titleLabel.setTextColor(overrideTextColor ?: DSSColors.success())
    }
}
