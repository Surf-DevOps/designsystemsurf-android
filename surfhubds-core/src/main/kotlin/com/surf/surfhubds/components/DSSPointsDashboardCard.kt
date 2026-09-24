package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
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

    /**
     * true (padrão): card com fundo, borda e padding próprios. false: só o conteúdo,
     * para ser embutido num container maior (ex.: card do dashboard com cupom e botão).
     */
    var showsBackground: Boolean = true
        set(value) {
            field = value
            val pad = if (value) 20f.dpToPx(context) else 0
            setPadding(pad, pad, pad, pad)
            refresh()
        }

    /** false esconde as barras de progresso (ex.: resumo do extrato no Figma). */
    var showsProgress: Boolean = true
        set(value) {
            field = value
            val visibility = if (value) View.VISIBLE else View.GONE
            availableProgressBar.visibility = visibility
            expiringProgressBar.visibility = visibility
        }

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
                addView(availableIcon, LayoutParams(18f.dpToPx(ctx), 18f.dpToPx(ctx)))
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
            
            // marginEnd: legenda longa ("junte pontos para trocar") quebra em vez de encostar
            // na coluna ao lado.
            addView(availableCaptionLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 6f.dpToPx(ctx)
                marginEnd = 16f.dpToPx(ctx)
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
                addView(expiringIcon, LayoutParams(18f.dpToPx(ctx), 18f.dpToPx(ctx)))
                addView(expiringValueLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginStart = 8f.dpToPx(ctx)
                })
            }
            addView(valueRow, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10f.dpToPx(ctx)
            })
            
            addView(expiringProgressBar, LayoutParams(LayoutParams.MATCH_PARENT, 8f.dpToPx(ctx)).apply {
                topMargin = 12f.dpToPx(ctx)
                marginEnd = 16f.dpToPx(ctx)
            })
            
            addView(expiringCaptionLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 6f.dpToPx(ctx)
                marginEnd = 16f.dpToPx(ctx)
            })
        }
        headerRow.addView(rightColumn, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        addView(headerRow)
        
        // Estilização (Figma): título 15 regular, valor 24 bold, legenda 14 regular
        listOf(availableTitleLabel, expiringTitleLabel).forEach {
            it.textSize = 15f
            it.typeface = DSSFont.regular(ctx, 15f).typeface
        }
        listOf(availableValueLabel, expiringValueLabel).forEach {
            it.textSize = 24f
            it.typeface = DSSFont.bold(ctx, 24f).typeface
        }
        listOf(availableCaptionLabel, expiringCaptionLabel).forEach {
            it.textSize = 14f
            it.typeface = DSSFont.regular(ctx, 14f).typeface
        }

        availableIcon.setImageResource(R.drawable.dss_ic_star_benefit_color)
        expiringIcon.setImageResource(R.drawable.dss_ic_calendar_clock)
    }

    fun configure(
        availableTitle: String,
        availableValue: String,
        availableCaption: String,
        availableProgress: Int,
        availableProgressMax: Int = 100,
        expiringTitle: String,
        expiringValue: String,
        expiringCaption: String,
        expiringProgress: Int,
        expiringProgressMax: Int = 100
    ) {
        availableTitleLabel.text = availableTitle
        availableValueLabel.text = availableValue
        availableCaptionLabel.text = availableCaption
        availableProgressBar.max = availableProgressMax.coerceAtLeast(1)
        availableProgressBar.progress = availableProgress.coerceIn(0, availableProgressBar.max)
        
        expiringTitleLabel.text = expiringTitle
        expiringValueLabel.text = expiringValue
        expiringCaptionLabel.text = expiringCaption
        expiringProgressBar.max = expiringProgressMax.coerceAtLeast(1)
        expiringProgressBar.progress = expiringProgress.coerceIn(0, expiringProgressBar.max)
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val ctx = context
        val scheme = ThemeManager.colorScheme
        val isDark = scheme == ColorScheme.DARK || scheme == ColorScheme.BLACK
        
        val bgColor = if (isDark) Color.rgb(28, 28, 30) else Color.WHITE
        background = if (showsBackground) DrawableFactory.rounded(
            context = ctx,
            backgroundColor = bgColor,
            cornerRadiusDp = 12f,
            strokeColor = DSSColors.divider(),
            strokeWidthDp = 1f
        ) else null

        availableTitleLabel.setTextColor(DSSColors.textSecondary())
        availableValueLabel.setTextColor(DSSColors.textPrimary())
        availableCaptionLabel.setTextColor(DSSColors.textSecondary())
        
        expiringTitleLabel.setTextColor(DSSColors.textSecondary())
        expiringValueLabel.setTextColor(DSSColors.textPrimary())
        expiringCaptionLabel.setTextColor(DSSColors.textSecondary())
        
        // Estrela é ilustração colorida (sem tint); calendário segue o primary.
        availableIcon.clearColorFilter()
        expiringIcon.setColorFilter(DSSColors.primary())

        // Barras montadas em runtime: os tokens seguem o tema e a paleta da brand,
        // ao contrario de um drawable XML de cor fixa.
        availableProgressBar.setProgressDrawableKeepingLevel(
            progressBarDrawable(fill = DSSColors.success())
        )
        expiringProgressBar.setProgressDrawableKeepingLevel(
            progressBarDrawable(fill = DSSColors.secondary())
        )
    }

    /** Track neutro + preenchimento arredondado, equivalente ao layer-list/clip do XML. */
    private fun progressBarDrawable(@ColorInt fill: Int): Drawable {
        val track = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.divider(),
            cornerRadiusDp = PROGRESS_CORNER_DP
        )
        val bar = ClipDrawable(
            DrawableFactory.rounded(
                context = context,
                backgroundColor = fill,
                cornerRadiusDp = PROGRESS_CORNER_DP
            ),
            Gravity.START,
            ClipDrawable.HORIZONTAL
        )
        return LayerDrawable(arrayOf(track, bar)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }

    /** Trocar o progressDrawable zera o nivel desenhado; reaplica o progresso corrente. */
    private fun ProgressBar.setProgressDrawableKeepingLevel(drawable: Drawable) {
        val current = progress
        progressDrawable = drawable
        progress = 0
        progress = current
    }

    private companion object {
        const val PROGRESS_CORNER_DP = 4f
    }
}
