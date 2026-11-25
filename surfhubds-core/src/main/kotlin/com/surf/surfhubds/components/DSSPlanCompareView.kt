package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.surf.surfhubds.R
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.weightParams
import com.surf.surfhubds.util.wrapParams

/**
 * Comparação "Plano atual → Novo plano" (Figma do "Confirmar troca de plano"): dois
 * cards lado a lado com rótulo, franquia ("Até 35GB") e preço, e uma seta no meio.
 * O card do novo plano fica destacado com a borda forte.
 */
class DSSPlanCompareView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    data class Plan(val label: CharSequence, val data: CharSequence, val price: CharSequence)

    private val currentCard = PlanBox(context, highlighted = false)
    private val newCard = PlanBox(context, highlighted = true)
    private val arrowHolder = FrameLayout(context)
    private val arrow = ImageView(context).apply {
        setImageDrawable(ContextCompat.getDrawable(context, R.drawable.dss_ic_arrow_right))
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val ctx = context
        val circle = 32f.dpToPx(ctx)
        val glyph = 16f.dpToPx(ctx)
        arrowHolder.addView(arrow, FrameLayout.LayoutParams(glyph, glyph, Gravity.CENTER))
        addView(currentCard, weightParams())
        addView(arrowHolder, LayoutParams(circle, circle).apply {
            val m = 8f.dpToPx(ctx)
            marginStart = m
            marginEnd = m
        })
        addView(newCard, weightParams())
        refresh()
        setupThemeObserver()
    }

    fun configure(current: Plan, new: Plan) {
        currentCard.bind(current)
        newCard.bind(new)
        contentDescription = "${current.label}: ${current.data} ${current.price}. ${new.label}: ${new.data} ${new.price}"
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        arrowHolder.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(DSSColors.background())
            setStroke(1f.dpToPx(context), DSSColors.divider())
        }
        arrow.setColorFilter(DSSColors.textPrimary())
        currentCard.refresh()
        newCard.refresh()
    }

    private class PlanBox(context: Context, private val highlighted: Boolean) : LinearLayout(context) {
        private val label = dssText(context, if (highlighted) DSSWeight.BOLD else DSSWeight.REGULAR, 13f, DSSColors.textSecondary(), maxLines = 1)
        private val data = dssText(context, DSSWeight.BOLD, 20f, DSSColors.textPrimary(), maxLines = 1)
        private val price = dssText(context, DSSWeight.BOLD, 15f, DSSColors.textPrimary(), maxLines = 1)

        init {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val v = 20f.dpToPx(context)
            setPadding(8f.dpToPx(context), v, 8f.dpToPx(context), v)
            addView(label, wrapParams())
            addView(data, wrapParams().apply { topMargin = 8f.dpToPx(context) })
            addView(price, wrapParams().apply { topMargin = 6f.dpToPx(context) })
            refresh()
        }

        fun bind(plan: Plan) {
            label.text = plan.label
            data.text = plan.data
            price.text = plan.price
        }

        fun refresh() {
            background = if (highlighted) {
                DrawableFactory.rounded(context, DSSColors.background(), 12f, DSSColors.textPrimary(), 2f)
            } else {
                DrawableFactory.rounded(context, DSSColors.backgroundSecondary(), 12f, DSSColors.divider(), 1f)
            }
            label.setTextColor(if (highlighted) DSSColors.textPrimary() else DSSColors.textSecondary())
            data.setTextColor(DSSColors.textPrimary())
            price.setTextColor(DSSColors.textPrimary())
        }
    }
}
