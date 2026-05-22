package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `BottomSheetWelcomeView` do iOS — view de boas-vindas com título, ilustração e MSISDN.
 */
class BottomSheetWelcomeView(context: Context) : FrameLayout(context), ThemeAware {

    private val titleLabel = TextView(context).apply {
        gravity = Gravity.CENTER
        typeface = DSSFont.bold(context, 24f).typeface
        textSize = 24f
    }

    private val imageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private val numberDescriptionLabel = TextView(context).apply {
        gravity = Gravity.CENTER
        text = "Seu número é:"
        typeface = DSSFont.medium(context, 18f).typeface
        textSize = 18f
    }

    private val msisdnLabel = TextView(context).apply {
        gravity = Gravity.CENTER
        typeface = DSSFont.bold(context, 20f).typeface
        textSize = 20f
    }

    private val stackView = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    private val numberStackView = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    init {
        addView(stackView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            leftMargin = 24f.dpToPx(context)
            rightMargin = 24f.dpToPx(context)
        })

        val spacingPx = 16f.dpToPx(context)

        stackView.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = spacingPx })

        stackView.addView(imageView, LinearLayout.LayoutParams(
            280f.dpToPx(context), 200f.dpToPx(context),
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = spacingPx
        })

        numberStackView.addView(numberDescriptionLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = 4f.dpToPx(context) })
        numberStackView.addView(msisdnLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        stackView.addView(numberStackView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { gravity = Gravity.CENTER_HORIZONTAL })

        refresh()
        setupThemeObserver()
    }

    fun configure(title: String, image: Drawable?, msisdn: String) {
        titleLabel.text = title
        imageView.setImageDrawable(image)
        msisdnLabel.text = msisdn
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackgroundColor(DSSColors.background())
        titleLabel.setTextColor(DSSColors.textPrimary())
        numberDescriptionLabel.setTextColor(DSSColors.textPrimary())
        msisdnLabel.setTextColor(DSSColors.error())
    }
}
