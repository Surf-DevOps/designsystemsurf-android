package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
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
 * Port do `DSSScheduleOfferCardView.swift` (iOS) — card simples de oferta agendada.
 *
 * Mostra header + nome do plano + preço, badge customizável e (opcional) data.
 * Abaixo do card, uma descrição.
 */
class DSSScheduleOfferCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    private val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val card = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val headerLabel = TextView(context).apply {
        text = "Sua oferta"
        textSize = 16f
        typeface = DSSFont.light(context, 16f).typeface
        // iOS: numberOfLines padrão = 1
        maxLines = 1
    }
    private val planNameLabel = TextView(context).apply {
        textSize = 20f
        typeface = DSSFont.bold(context, 20f).typeface
        // iOS: numberOfLines padrão = 1 (priceLabel é compression-resistant) -> trunca no fim
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    private val priceLabel = TextView(context).apply { gravity = Gravity.END; maxLines = 1 }
    private val badge = TextView(context).apply {
        textSize = 13f
        typeface = DSSFont.light(context, 13f).typeface
        gravity = Gravity.CENTER
        // iOS: badgeLabel numberOfLines padrão = 1
        maxLines = 1
        setPadding(16f.dpToPx(context), 4f.dpToPx(context), 16f.dpToPx(context), 4f.dpToPx(context))
    }
    private val dateLabel = TextView(context).apply {
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
        // iOS: numberOfLines padrão = 1
        maxLines = 1
        visibility = View.GONE
    }
    private val descriptionLabel = TextView(context).apply {
        textSize = 15f
        typeface = DSSFont.light(context, 15f).typeface
        // iOS: numberOfLines = 0 (sem limite)
        setSingleLine(false)
        maxLines = Int.MAX_VALUE
    }

    init {
        val hPad = 24f.dpToPx(context); val vPad = 20f.dpToPx(context)
        card.setPadding(hPad, vPad, hPad, vPad)
        card.addView(headerLabel)

        val nameRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(planNameLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        nameRow.addView(priceLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        card.addView(
            nameRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 8f.dpToPx(context) },
        )
        card.addView(
            badge,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                28f.dpToPx(context),
            ).apply { topMargin = 12f.dpToPx(context) },
        )
        card.addView(
            dateLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 4f.dpToPx(context) },
        )

        column.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        column.addView(
            descriptionLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 16f.dpToPx(context)
                leftMargin = 4f.dpToPx(context); rightMargin = 4f.dpToPx(context)
            },
        )
        addView(column)
        refresh()
        setupThemeObserver()
    }

    /**
     * Configura o card.
     * @param date Quando null, a linha de data fica oculta.
     */
    fun configure(
        planName: String,
        priceCents: Int,
        badgeText: String,
        date: String? = null,
        descriptionText: String,
    ) {
        planNameLabel.text = planName
        badge.text = badgeText
        descriptionLabel.text = descriptionText
        val reais = priceCents / 100
        priceLabel.text = "R$$reais/mês"
        priceLabel.typeface = DSSFont.bold(context, 16f).typeface
        priceLabel.textSize = 16f
        if (date != null) {
            dateLabel.text = date
            dateLabel.visibility = View.VISIBLE
        } else {
            dateLabel.visibility = View.GONE
        }
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        card.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.backgroundSecondary(),
            cornerRadiusDp = 16f,
        )
        headerLabel.setTextColor(DSSColors.textSecondary())
        planNameLabel.setTextColor(DSSColors.textPrimary())
        priceLabel.setTextColor(DSSColors.textPrimary())
        dateLabel.setTextColor(DSSColors.textPrimary())
        descriptionLabel.setTextColor(DSSColors.textPrimary())
        badge.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.primary(),
            cornerRadiusDp = 14f,
        )
        badge.setTextColor(DSSColors.textOnPrimary())
    }
}
