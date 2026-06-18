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
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Tela de confirmação da troca da data da recarga programada ("Data da programada").
 * Mostra a programada atual e a nova data em dois cards e dispara [Delegate.onConfirm] no botão
 * Confirmar — o app decide chamar a troca de data (updateBillingDay) ou a recarga (createPayment).
 */
class DSSScheduleDateConfirmView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    interface Delegate {
        fun onConfirm(view: DSSScheduleDateConfirmView)
    }

    var delegate: Delegate? = null

    companion object {
        /** Título da navbar desta tela (texto fica no DSS, usado pelo app ao configurar a navbar). */
        const val TITLE = "Data da programada"
    }

    private val questionLabel = TextView(context).apply {
        text = AppStrings.brand(context, "schedule_date_confirm_question", "Confirma a troca da data de cobrança da recarga programada?")
        textSize = 14f
        typeface = DSSFont.bold(context, 14f).typeface
    }

    private val currentTitle = TextView(context).apply {
        text = AppStrings.brand(context, "schedule_date_confirm_current", "Programada atual:")
        textSize = 13f
        typeface = DSSFont.light(context, 13f).typeface
    }
    private val currentCard = LinearLayout(context).apply { orientation = VERTICAL }
    private val currentPlanBadge = badge()
    private val currentDateText = TextView(context).apply { textSize = 12f; gravity = Gravity.END; typeface = DSSFont.light(context, 12f).typeface }
    private val currentPhoneText = TextView(context).apply { textSize = 13f; typeface = DSSFont.light(context, 13f).typeface }
    private val currentValueText = TextView(context).apply { textSize = 14f; gravity = Gravity.END; typeface = DSSFont.bold(context, 14f).typeface }

    private val newTitle = TextView(context).apply {
        text = AppStrings.brand(context, "schedule_date_confirm_new", "Nova data:")
        textSize = 14f
        typeface = DSSFont.bold(context, 14f).typeface
    }
    private val newCard = LinearLayout(context).apply { orientation = VERTICAL }
    private val newPlanBadge = badge()
    private val newDateBadge = badge()
    private val newPhoneText = TextView(context).apply { textSize = 13f; typeface = DSSFont.light(context, 13f).typeface }
    private val newValueText = TextView(context).apply { textSize = 14f; gravity = Gravity.END; typeface = DSSFont.bold(context, 14f).typeface }

    private val confirmButton = DSSPrincipalButton(context).apply {
        text = AppStrings.brand(context, "common_confirm", "Confirmar")
        typeface = DSSFont.regular(context, 16f).typeface
    }

    init {
        orientation = VERTICAL
        val pad = 24f.dpToPx(context)
        setPadding(pad, 20f.dpToPx(context), pad, pad)

        addView(questionLabel, vlp().apply { topMargin = 4f.dpToPx(context) })
        addView(currentTitle, vlp().apply { topMargin = 16f.dpToPx(context) })
        setupCard(currentCard, currentPlanBadge, currentDateText, currentPhoneText, currentValueText)
        addView(currentCard, vlp().apply { topMargin = 6f.dpToPx(context) })
        addView(newTitle, vlp().apply { topMargin = 16f.dpToPx(context) })
        setupCard(newCard, newPlanBadge, newDateBadge, newPhoneText, newValueText)
        addView(newCard, vlp().apply { topMargin = 6f.dpToPx(context) })
        addView(View(context), LayoutParams(LayoutParams.MATCH_PARENT, 0).apply { weight = 1f })
        addView(
            confirmButton,
            LayoutParams(LayoutParams.MATCH_PARENT, 50f.dpToPx(context)).apply { topMargin = 16f.dpToPx(context) },
        )

        confirmButton.onTap = { delegate?.onConfirm(this) }
        refresh()
        setupThemeObserver()
    }

    private fun badge() = TextView(context).apply {
        textSize = 12f
        typeface = DSSFont.medium(context, 12f).typeface
        setPadding(8f.dpToPx(context), 3f.dpToPx(context), 8f.dpToPx(context), 3f.dpToPx(context))
    }

    private fun vlp() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    private fun setupCard(card: LinearLayout, planBadge: TextView, rightTop: TextView, phone: TextView, value: TextView) {
        val p = 12f.dpToPx(context)
        card.setPadding(p, p, p, p)
        val row1 = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row1.addView(planBadge, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        row1.addView(View(context), LayoutParams(0, 1).apply { weight = 1f })
        row1.addView(rightTop, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        card.addView(row1, vlp())

        val row2 = LinearLayout(context).apply { orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row2.addView(phone, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row2.addView(value, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        card.addView(row2, vlp().apply { topMargin = 10f.dpToPx(context) })
    }

    /**
     * @param currentDate / [newDate] já formatados (ex.: "24/07/26"). O mesmo plano/valor/telefone
     * aparece nos dois cards (só a data muda).
     */
    fun configure(
        planName: String,
        value: String,
        msisdn: String,
        currentDate: String,
        newDate: String,
    ) {
        currentPlanBadge.text = planName
        currentPhoneText.text = msisdn
        currentValueText.text = value
        currentDateText.text = "Programada: $currentDate"

        newPlanBadge.text = planName
        newPhoneText.text = msisdn
        newValueText.text = value
        newDateBadge.text = "Programada: $newDate"
    }

    fun setConfirmLoading(loading: Boolean) {
        confirmButton.isEnabled = !loading
        confirmButton.alpha = if (loading) 0.6f else 1f
    }

    override fun applyTheme(theme: Theme) = refresh()

    private fun refresh() {
        setBackgroundColor(DSSColors.background())
        questionLabel.setTextColor(DSSColors.textPrimary())
        currentTitle.setTextColor(DSSColors.textPrimary())
        newTitle.setTextColor(DSSColors.primary())

        listOf(currentDateText, currentPhoneText, currentValueText, newPhoneText, newValueText)
            .forEach { it.setTextColor(DSSColors.textPrimary()) }

        currentCard.background = DrawableFactory.rounded(context, DSSColors.backgroundSecondary(), 12f)
        newCard.background = DrawableFactory.rounded(
            context,
            DSSColors.background(),
            12f,
            strokeColor = DSSColors.primary(),
            strokeWidthDp = 1.5f,
        )

        currentPlanBadge.background = DrawableFactory.rounded(context, DSSColors.backgroundSecondary(), 6f)
        currentPlanBadge.setTextColor(DSSColors.textPrimary())
        newPlanBadge.background = DrawableFactory.rounded(context, DSSColors.backgroundSecondary(), 6f)
        newPlanBadge.setTextColor(DSSColors.textPrimary())

        // Badge da nova data em destaque (primary), igual ao iOS/print.
        // Badge (não botão) sobre fill primary: no dark/black o primary vira branco -> usa
        // contraste p/ o texto não sumir (mesma regra do badge "Programada" dos demais sheets).
        newDateBadge.background = DrawableFactory.rounded(context, DSSColors.primary(), 6f)
        newDateBadge.setTextColor(DSSColors.contrastOnPrimary())
    }
}
