package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.surf.surfhubds.R
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Linha da tabela de troca de pontos (Figma "Vale quanto"): barra | pacote e validade |
 * estrela, custo e status | check. Elegível = barra, status e check em `success`;
 * inelegível = pacote e barra apagados, sem check. [showsDivider] liga a linha de baixo.
 */
class DSSPlanPointsRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    private var eligible = true
    private var pointsValue = ""
    private var pointsUnit = ""

    private val bar = View(context)
    private val nameLabel = TextView(context).apply {
        textSize = 20f
        typeface = DSSFont.bold(context, 20f).typeface
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    private val validityLabel = TextView(context).apply {
        textSize = 13f
        typeface = DSSFont.regular(context, 13f).typeface
    }
    private val star = ImageView(context).apply { setImageResource(R.drawable.dss_ic_star_benefit_large) }
    private val pointsLabel = TextView(context)
    private val statusLabel = TextView(context).apply {
        textSize = 12f
        typeface = DSSFont.bold(context, 12f).typeface
    }
    private val check = DSSIconBadge(context)
    private val divider = View(context)

    var showsDivider: Boolean = true
        set(value) { field = value; divider.isVisible = value }

    init {
        orientation = VERTICAL
        val ctx = context
        val padH = 20f.dpToPx(ctx)
        setPadding(padH, 0, padH, 0)

        val row = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padV = 16f.dpToPx(ctx)
            setPadding(0, padV, 0, padV)
        }
        row.addView(bar, LayoutParams(3f.dpToPx(ctx), 44f.dpToPx(ctx)))
        row.addView(LinearLayout(ctx).apply {
            orientation = VERTICAL
            addView(nameLabel)
            addView(validityLabel)
        }, LayoutParams(84f.dpToPx(ctx), LayoutParams.WRAP_CONTENT).apply { marginStart = 14f.dpToPx(ctx) })
        row.addView(LinearLayout(ctx).apply {
            orientation = VERTICAL
            addView(LinearLayout(ctx).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(star, LayoutParams(18f.dpToPx(ctx), 18f.dpToPx(ctx)).apply { marginEnd = 6f.dpToPx(ctx) })
                addView(pointsLabel)
            })
            addView(statusLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = 2f.dpToPx(ctx)
            })
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row.addView(check, LayoutParams(20f.dpToPx(ctx), 20f.dpToPx(ctx)).apply { marginStart = 8f.dpToPx(ctx) })
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(divider, LayoutParams(LayoutParams.MATCH_PARENT, 1f.dpToPx(ctx)))

        check.setImageResource(R.drawable.dss_ic_check)
        check.autoPadding = false
        val checkPad = 4f.dpToPx(ctx)
        check.setPadding(checkPad, checkPad, checkPad, checkPad)
        refresh()
        setupThemeObserver()
    }

    /**
     * [points] é o número já formatado ("1.250") e [pointsUnit] a unidade ("pontos");
     * [status] é "Dá para resgatar" ou "faltam X pontos", conforme [isEligible].
     */
    fun configure(
        name: String,
        validity: String,
        points: String,
        pointsUnit: String,
        status: String,
        isEligible: Boolean,
    ) {
        nameLabel.text = name
        validityLabel.text = validity
        pointsValue = points
        this.pointsUnit = pointsUnit
        statusLabel.text = status
        eligible = isEligible
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        val accent = if (eligible) DSSColors.success() else DSSColors.divider()
        bar.background = DrawableFactory.rounded(context, accent, 2f)
        nameLabel.setTextColor(if (eligible) DSSColors.textPrimary() else DSSColors.textSecondary())
        validityLabel.setTextColor(DSSColors.textSecondary())
        statusLabel.setTextColor(if (eligible) DSSColors.success() else DSSColors.textSecondary())
        check.style = DSSIconBadge.Style.SUCCESS
        check.visibility = if (eligible) VISIBLE else INVISIBLE
        divider.setBackgroundColor(DSSColors.dividerStrong())
        // "250 pontos": número 15 bold + unidade 13 regular em textSecondary.
        pointsLabel.setTextColor(DSSColors.textPrimary())
        pointsLabel.text = SpannableString("$pointsValue $pointsUnit").apply {
            val n = pointsValue.length
            setSpan(StyleSpan(Typeface.BOLD), 0, n, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(AbsoluteSizeSpan(15, true), 0, n, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(AbsoluteSizeSpan(13, true), n, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(DSSColors.textSecondary()), n, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
