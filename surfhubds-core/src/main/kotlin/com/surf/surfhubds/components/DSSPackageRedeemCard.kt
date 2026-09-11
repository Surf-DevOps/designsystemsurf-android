package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRadioButton
import com.surf.surfhubds.R
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Componente de card para seleção de pacote de bônus.
 * Exibe informações de internet e custo em pontos, com um rádio button para seleção.
 */
class DSSPackageRedeemCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    private val internetTitleLabel = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.regular(context, 14f).typeface
    }
    private val internetValueLabel = TextView(context).apply {
        textSize = 24f
        typeface = DSSFont.bold(context, 24f).typeface
    }
    private val validityLabel = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.regular(context, 14f).typeface
    }

    private val costTitleLabel = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.regular(context, 14f).typeface
    }
    private val starIcon = ImageView(context).apply {
        setImageResource(R.drawable.dss_ic_star_benefit)
    }
    private val pointsValueLabel = TextView(context).apply {
        textSize = 24f
        typeface = DSSFont.bold(context, 24f).typeface
    }
    private val pointsCaptionLabel = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.regular(context, 14f).typeface
    }

    private val radioButton = AppCompatRadioButton(context).apply {
        isClickable = false
        isFocusable = false
    }

    private var _selected = false
    private var _eligible = true

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = 20f.dpToPx(context)
        setPadding(pad, pad, pad, pad)

        // Coluna Internet
        val leftColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(internetTitleLabel)
            addView(internetValueLabel)
            addView(validityLabel)
        }
        addView(leftColumn, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.45f))

        // Coluna Custo
        val rightColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(costTitleLabel)
            
            val pointsRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(starIcon, LayoutParams(16f.dpToPx(context), 16f.dpToPx(context)))
                addView(pointsValueLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginStart = 6f.dpToPx(context)
                })
            }
            addView(pointsRow)
            addView(pointsCaptionLabel)
        }
        addView(rightColumn, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.45f))

        // Radio Button
        addView(radioButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        refresh()
        setupThemeObserver()
    }

    fun configure(
        internetTitle: String,
        internetValue: String,
        validityText: String,
        costTitle: String,
        pointsValue: String,
        pointsCaption: String,
        isEligible: Boolean = true,
        isSelected: Boolean = false
    ) {
        internetTitleLabel.text = internetTitle
        internetValueLabel.text = internetValue
        validityLabel.text = validityText
        costTitleLabel.text = costTitle
        pointsValueLabel.text = pointsValue
        pointsCaptionLabel.text = pointsCaption
        _eligible = isEligible
        _selected = isSelected
        refresh()
    }

    fun setSelectedState(selected: Boolean) {
        if (!_eligible && selected) return
        _selected = selected
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val ctx = context
        val scheme = ThemeManager.colorScheme
        val isDark = scheme == ColorScheme.DARK || scheme == ColorScheme.BLACK
        
        internetTitleLabel.setTextColor(DSSColors.textSecondary())
        internetValueLabel.setTextColor(DSSColors.textPrimary())
        validityLabel.setTextColor(DSSColors.textSecondary())
        
        costTitleLabel.setTextColor(DSSColors.textSecondary())
        pointsValueLabel.setTextColor(DSSColors.textPrimary())
        pointsCaptionLabel.setTextColor(if (_eligible) DSSColors.textSecondary() else DSSColors.error())
        
        starIcon.setColorFilter(DSSColors.primary())
        radioButton.isChecked = _selected

        val bgColor = if (isDark) Color.rgb(28, 28, 30) else Color.WHITE
        
        background = if (_selected) {
            DrawableFactory.rounded(
                context = ctx,
                backgroundColor = bgColor,
                cornerRadiusDp = 12f,
                strokeColor = DSSColors.primary(),
                strokeWidthDp = 2f
            )
        } else {
            DrawableFactory.rounded(
                context = ctx,
                backgroundColor = bgColor,
                cornerRadiusDp = 12f,
                strokeColor = DSSColors.divider(),
                strokeWidthDp = 1f
            )
        }

        alpha = if (_eligible) 1f else 0.5f
    }
}
