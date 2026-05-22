package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
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
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSScheduleEmptyStateView.swift` (iOS) — empty state da tela de recarga programada.
 */
class DSSScheduleEmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    var onScheduleRechargeTapped: (() -> Unit)? = null

    var iconDrawable: android.graphics.drawable.Drawable? = null
        set(value) { field = value; iconImage.setImageDrawable(value) }

    private val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }
    private val iconImage = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    private val titleLabel = TextView(context).apply {
        text = "Você não possui uma recarga \nprogramada na sua linha.\nPrograme agora e aproveite!"
        textSize = 16f
        typeface = DSSFont.regular(context, 16f).typeface
        gravity = Gravity.START
    }
    private val benefitsStack = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.START
    }
    private val scheduleButton = DSSPrincipalButton(context).apply {
        text = "Programar recarga"
        textSize = 17f
    }

    init {
        column.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.CENTER
        }
        val hPad = 24f.dpToPx(context)
        column.setPadding(hPad, hPad, hPad, hPad)

        column.addView(
            iconImage,
            LinearLayout.LayoutParams(100f.dpToPx(context), 100f.dpToPx(context)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        column.addView(
            titleLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 24f.dpToPx(context) },
        )

        for (text in listOf("GB bônus", "Renovações automáticas", "Mais praticidade")) {
            val l = TextView(context).apply {
                this.text = "✓ $text"
                textSize = 18f
            }
            benefitsStack.addView(l)
        }
        column.addView(
            benefitsStack,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 24f.dpToPx(context) },
        )
        scheduleButton.onTap = { onScheduleRechargeTapped?.invoke() }
        column.addView(
            scheduleButton,
            LinearLayout.LayoutParams(
                320f.dpToPx(context),
                50f.dpToPx(context),
            ).apply {
                topMargin = 32f.dpToPx(context)
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )

        addView(column)
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackgroundColor(DSSColors.background())
        titleLabel.setTextColor(DSSColors.textPrimary())
        for (i in 0 until benefitsStack.childCount) {
            (benefitsStack.getChildAt(i) as? TextView)?.setTextColor(DSSColors.primary())
        }
    }
}
