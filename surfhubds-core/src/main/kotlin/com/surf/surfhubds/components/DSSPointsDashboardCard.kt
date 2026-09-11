package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
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
 * Card principal do dashboard do Compre e Ganhe.
 * Mostra saldo disponível, pontos a vencer e progresso.
 */
class DSSPointsDashboardCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    // Seção Saldo
    private val availableTitleLabel = TextView(context)
    private val availableIcon = ImageView(context)
    private val availableValueLabel = TextView(context)
    private val availableProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
    private val availableCaptionLabel = TextView(context)

    // Seção A Vencer
    private val expiringTitleLabel = TextView(context)
    private val expiringIcon = ImageView(context)
    private val expiringValueLabel = TextView(context)
    private val expiringProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
    private val expiringCaptionLabel = TextView(context)

    init {
        orientation = VERTICAL
        val pad = 20f.dpToPx(context)
        setPadding(pad, pad, pad, pad)

        setupUI()
        refresh()
        setupThemeObserver()
    }

    private fun setupUI() {
        val ctx = context
        
        // Linha Superior (Saldo e A Vencer)
        val headerRow = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
        }

        // Coluna Saldo
        val leftColumn = LinearLayout(ctx).apply {
            orientation = VERTICAL
            addView(availableTitleLabel)
            
            val valueRow = LinearLayout(ctx).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(availableIcon, LayoutParams(20f.dpToPx(ctx), 20f.dpToPx(ctx)))
                addView(availableValueLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginStart = 8f.dpToPx(ctx)
                })
            }
            addView(valueRow, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10f.dpToPx(ctx)
            })
            
            addView(availableProgressBar, LayoutParams(LayoutParams.MATCH_PARENT, 8f.dpToPx(ctx)).apply {
                topMargin = 12f.dpToPx(ctx)
                marginEnd = 16f.dpToPx(ctx)
            })
            
            addView(availableCaptionLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 6f.dpToPx(ctx)
            })
        }
        headerRow.addView(leftColumn, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        // Coluna A Vencer
        val rightColumn = LinearLayout(ctx).apply {
            orientation = VERTICAL
            addView(expiringTitleLabel)
            
            val valueRow = LinearLayout(ctx).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(expiringIcon, LayoutParams(20f.dpToPx(ctx), 20f.dpToPx(ctx)))
                addView(expiringValueLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginStart = 8f.dpToPx(ctx)
                })
            }
            addView(valueRow, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10f.dpToPx(ctx)
            })
            
            addView(expiringProgressBar, LayoutParams(LayoutParams.MATCH_PARENT, 8f.dpToPx(ctx)).apply {
                topMargin = 12f.dpToPx(ctx)
            })
            
            addView(expiringCaptionLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 6f.dpToPx(ctx)
            })
        }
        headerRow.addView(rightColumn, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        addView(headerRow)
        
        // Estilização Básica
        availableTitleLabel.textSize = 14f
        availableTitleLabel.typeface = DSSFont.medium(ctx, 14f).typeface
        
        availableValueLabel.textSize = 26f
        availableValueLabel.typeface = DSSFont.bold(ctx, 26f).typeface
        
        availableCaptionLabel.textSize = 14f
        availableCaptionLabel.typeface = DSSFont.regular(ctx, 14f).typeface
        
        expiringTitleLabel.textSize = 14f
        expiringTitleLabel.typeface = DSSFont.medium(ctx, 14f).typeface
        
        expiringValueLabel.textSize = 26f
        expiringValueLabel.typeface = DSSFont.bold(ctx, 26f).typeface
        
        expiringCaptionLabel.textSize = 14f
        expiringCaptionLabel.typeface = DSSFont.regular(ctx, 14f).typeface

        availableIcon.setImageResource(R.drawable.dss_ic_star_benefit)
        expiringIcon.setImageResource(R.drawable.dss_ic_calendar_clock)
    }

    fun configure(
        availableTitle: String,
        availableValue: String,
        availableCaption: String,
        availableProgress: Int,
        expiringTitle: String,
        expiringValue: String,
        expiringCaption: String,
        expiringProgress: Int
    ) {
        availableTitleLabel.text = availableTitle
        availableValueLabel.text = availableValue
        availableCaptionLabel.text = availableCaption
        availableProgressBar.progress = availableProgress
        
        expiringTitleLabel.text = expiringTitle
        expiringValueLabel.text = expiringValue
        expiringCaptionLabel.text = expiringCaption
        expiringProgressBar.progress = expiringProgress
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val ctx = context
        val scheme = ThemeManager.colorScheme
        val isDark = scheme == ColorScheme.DARK || scheme == ColorScheme.BLACK
        
        val bgColor = if (isDark) Color.rgb(28, 28, 30) else Color.WHITE
        background = DrawableFactory.rounded(
            context = ctx,
            backgroundColor = bgColor,
            cornerRadiusDp = 12f,
            strokeColor = DSSColors.divider(),
            strokeWidthDp = 1f
        )

        availableTitleLabel.setTextColor(DSSColors.textSecondary())
        availableValueLabel.setTextColor(DSSColors.textPrimary())
        availableCaptionLabel.setTextColor(DSSColors.textSecondary())
        
        expiringTitleLabel.setTextColor(DSSColors.textSecondary())
        expiringValueLabel.setTextColor(DSSColors.textPrimary())
        expiringCaptionLabel.setTextColor(DSSColors.textSecondary())
        
        availableIcon.setColorFilter(DSSColors.primary())
        expiringIcon.setColorFilter(DSSColors.secondary())

        // Progress Bars coloring
        availableProgressBar.progressDrawable = ctx.getDrawable(R.drawable.dss_points_progress_green)
        expiringProgressBar.progressDrawable = ctx.getDrawable(R.drawable.dss_points_progress_orange)
    }
}
