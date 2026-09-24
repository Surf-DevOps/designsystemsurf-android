package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
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
 * Linha de movimentação de pontos (Figma do Compre e Ganhe): barra colorida à esquerda,
 * data, título, detalhe e os pontos à direita. A cor da barra e dos pontos vem do [Kind]:
 * ganho = `success`, perda (vencimento) = `error`, neutro (resgate) = `textSecondary`.
 */
class DSSMovementRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    enum class Kind { GAIN, NEUTRAL, LOSS }

    private var kind: Kind = Kind.GAIN

    private val bar = View(context)
    private val dateLabel = label(12f, bold = false)
    private val titleLabel = label(15f, bold = true)
    private val detailLabel = label(12f, bold = false)
    private val pointsLabel = label(14f, bold = true)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padH = 16f.dpToPx(context)
        val padV = 12f.dpToPx(context)
        setPadding(padH, padV, padH, padV)

        // A barra acompanha a altura do bloco de textos (MATCH_PARENT na linha).
        addView(bar, LayoutParams(3f.dpToPx(context), LayoutParams.MATCH_PARENT))
        val texts = LinearLayout(context).apply {
            orientation = VERTICAL
            addView(dateLabel)
            addView(titleLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 2f.dpToPx(context)
            })
            addView(detailLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 2f.dpToPx(context)
            })
        }
        addView(texts, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = 12f.dpToPx(context)
            marginEnd = 8f.dpToPx(context)
        })
        addView(pointsLabel)

        refresh()
        setupThemeObserver()
    }

    fun configure(date: String, title: String, detail: String, points: String, kind: Kind) {
        dateLabel.text = date
        titleLabel.text = title
        detailLabel.text = detail
        pointsLabel.text = points
        this.kind = kind
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val accent = when (kind) {
            Kind.GAIN -> DSSColors.success()
            Kind.LOSS -> DSSColors.error()
            Kind.NEUTRAL -> DSSColors.textSecondary()
        }
        bar.background = DrawableFactory.rounded(context, accent, 2f)
        pointsLabel.setTextColor(accent)
        dateLabel.setTextColor(DSSColors.textSecondary())
        titleLabel.setTextColor(DSSColors.textPrimary())
        detailLabel.setTextColor(DSSColors.textSecondary())
    }

    private fun label(sizeSp: Float, bold: Boolean) = TextView(context).apply {
        textSize = sizeSp
        typeface = if (bold) DSSFont.bold(context, sizeSp).typeface else DSSFont.regular(context, sizeSp).typeface
    }
}
