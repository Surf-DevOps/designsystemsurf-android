package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.transition.TransitionManager
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `ExpandablePlanCardView.swift` (iOS) — card de oferta atual com seção colapsável de features.
 *
 * Reaproveita [DSSPlanCollectionView.CheckListItem] para itens das seções
 * "Ilimitados" / "Assinaturas".
 */
class ExpandablePlanCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    data class Feature(val title: String, val description: String)

    data class PlanData(
        val planName: String,
        val priceCents: Int,
        val parcelas: Int = 1,
        val sms: String,
        val voz: String,
        val internetDetail: List<Pair<String, String>> = emptyList(),
        val unlimitedItems: List<DSSPlanCollectionView.CheckListItem> = emptyList(),
        val subscriptionItems: List<DSSPlanCollectionView.CheckListItem> = emptyList(),
    )

    private val cardBackground = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val contentColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val currentOfferLabel = TextView(context)
    private val planNameLabel = TextView(context)
    private val priceLabel = TextView(context)
    private val validityBadge = TextView(context)
    private val validityDateLabel = TextView(context)

    private val benefitsStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val ilimitadosTitle = TextView(context)
    private val ilimitadosStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val subscriptionsTitle = TextView(context)
    private val subscriptionsStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val featuresStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val expandableContent = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
    private val toggleButton = TextView(context)

    private var isExpanded = false

    init {
        setupHierarchy()
        setupStyles()
        toggleButton.setOnClickListener { toggle() }
        refresh()
        setupThemeObserver()
    }

    private fun setupHierarchy() {
        cardBackground.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val pad = 16f.dpToPx(context)
        contentColumn.setPadding(pad, pad, pad, pad)

        // Header
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val planNameLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        headerRow.addView(planNameLabel, planNameLp)
        headerRow.addView(
            priceLabel,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )

        // Validity row
        val validityRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        validityRow.addView(validityBadge)
        val spacer = View(context)
        validityRow.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        validityRow.addView(validityDateLabel)

        contentColumn.addView(currentOfferLabel)
        contentColumn.addView(headerRow, marginTop(8))
        contentColumn.addView(validityRow, marginTop(12))
        contentColumn.addView(benefitsStack, marginTop(16))
        contentColumn.addView(ilimitadosTitle, marginTop(16))
        contentColumn.addView(ilimitadosStack, marginTop(8))
        contentColumn.addView(subscriptionsTitle, marginTop(16))
        contentColumn.addView(subscriptionsStack, marginTop(8))
        contentColumn.addView(expandableContent, marginTop(16))
        expandableContent.addView(featuresStack)

        cardBackground.addView(contentColumn)
        cardBackground.addView(
            toggleButton,
            LinearLayout.LayoutParams(140f.dpToPx(context), 40f.dpToPx(context)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 8f.dpToPx(context)
                bottomMargin = 16f.dpToPx(context)
            },
        )

        addView(cardBackground)
    }

    private fun marginTop(dp: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp.toFloat().dpToPx(context) }

    private fun setupStyles() {
        currentOfferLabel.apply {
            text = "Oferta atual"
            textSize = 14f
            typeface = DSSFont.light(context, 14f).typeface
        }
        planNameLabel.apply { textSize = 18f; typeface = DSSFont.bold(context, 18f).typeface }
        priceLabel.apply { textSize = 18f; typeface = DSSFont.bold(context, 18f).typeface; gravity = Gravity.END }
        validityBadge.apply {
            text = "validade"
            textSize = 12f
            typeface = DSSFont.bold(context, 12f).typeface
            setPadding(12f.dpToPx(context), 4f.dpToPx(context), 12f.dpToPx(context), 4f.dpToPx(context))
        }
        validityDateLabel.apply { textSize = 14f; typeface = DSSFont.light(context, 14f).typeface; gravity = Gravity.END }
        ilimitadosTitle.apply { text = "Ilimitados"; textSize = 16f; typeface = DSSFont.bold(context, 16f).typeface }
        subscriptionsTitle.apply { text = "Assinaturas"; textSize = 16f; typeface = DSSFont.bold(context, 16f).typeface }
        toggleButton.apply {
            text = "Mais detalhes"
            textSize = 14f
            typeface = DSSFont.regular(context, 14f).typeface
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
        }
    }

    fun configure(
        plan: PlanData,
        validityDays: Int,
        hasScheduledRechargeBonus: Boolean,
        isScheduledRechargeActive: Boolean,
        validityDate: String = "",
        scheduleBonusMB: Int = 0,
        portabilityBonusMB: Int = 0,
        mvnoName: String = "",
    ) {
        currentOfferLabel.text = if (hasScheduledRechargeBonus) {
            val status = if (isScheduledRechargeActive) "Ativa" else "Inativa"
            "Oferta atual | Recarga \nprogramada $status"
        } else {
            "Oferta atual"
        }
        planNameLabel.text = plan.planName

        val finalPriceCents = if (plan.parcelas > 1) plan.priceCents / plan.parcelas else plan.priceCents
        val reais = finalPriceCents / 100
        val cs = finalPriceCents % 100
        priceLabel.text = if (cs == 0) "R$$reais/mês" else "R$${DSSPlanCollectionView.formatPrice(finalPriceCents)}/mês"
        validityDateLabel.text = validityDate

        // Benefits checkmarks
        benefitsStack.removeAllViews()
        for ((qt, no) in plan.internetDetail) benefitsStack.addView(checkmarkRow("$qt $no"))
        benefitsStack.addView(checkmarkRow(plan.sms))
        if (!plan.voz.lowercase().contains("ilimitad")) benefitsStack.addView(checkmarkRow(plan.voz))

        // Ilimitados
        ilimitadosStack.removeAllViews()
        val hasUnlimited = plan.unlimitedItems.isNotEmpty() ||
            plan.voz.lowercase().contains("ilimitad")
        ilimitadosTitle.visibility = if (hasUnlimited) View.VISIBLE else View.GONE
        ilimitadosStack.visibility = if (hasUnlimited) View.VISIBLE else View.GONE
        for (item in plan.unlimitedItems) ilimitadosStack.addView(iconRow(item))
        if (plan.voz.lowercase().contains("ilimitad")) {
            ilimitadosStack.addView(iconRow(DSSPlanCollectionView.CheckListItem("Ligações usando o código 41", null)))
        }

        // Subscriptions
        subscriptionsStack.removeAllViews()
        val hasSubs = plan.subscriptionItems.isNotEmpty()
        subscriptionsTitle.visibility = if (hasSubs) View.VISIBLE else View.GONE
        subscriptionsStack.visibility = if (hasSubs) View.VISIBLE else View.GONE
        for (item in plan.subscriptionItems) subscriptionsStack.addView(iconRow(item))

        // Features
        featuresStack.removeAllViews()
        val scheduleGB = scheduleBonusMB / 1024
        val portabilityGB = portabilityBonusMB / 1024
        addFeature(Feature("Internet que acumula", "A internet que você não utilizou acumula para o próximo mês. Basta manter sua oferta ativa."))
        addFeature(Feature("Ligações ilimitadas", "Ligações ilimitadas para qualquer operadora e em todo Brasil utilizando o código 41."))
        addFeature(Feature("Internet sem cortes", "Durante a validade da sua oferta você não fica sem internet (mesmo se consumir todos os GB do seu plano). Nós mantemos sua navegação liberada porém com velocidade reduzida até a próxima recarga."))
        addFeature(Feature("Validade do plano $validityDays dias", ""))
        if (hasScheduledRechargeBonus) {
            addFeature(Feature("Bônus recarga programada", "Os ${scheduleGB}GB bônus por recarga programada são adicionados todo mês ao seu plano enquanto tiver uma recarga programada ativa."))
        }
        if (portabilityGB > 0) {
            addFeature(Feature("Bônus portabilidade", "Os ${portabilityGB}GB bônus por portabilidade são adicionado todo mês no seu plano após trazer seu número de outra operadora para o $mvnoName."))
        }

        refresh()
    }

    private fun addFeature(feature: Feature) {
        val block = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(context).apply {
            text = feature.title
            textSize = 16f
            typeface = DSSFont.bold(context, 16f).typeface
            setTextColor(DSSColors.textPrimary())
        }
        block.addView(title)
        if (feature.description.isNotEmpty()) {
            val desc = TextView(context).apply {
                text = feature.description
                textSize = 14f
                typeface = DSSFont.regular(context, 14f).typeface
                setTextColor(DSSColors.textSecondary())
            }
            block.addView(
                desc,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 8f.dpToPx(context) },
            )
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 16f.dpToPx(context) }
        featuresStack.addView(block, lp)
    }

    private fun checkmarkRow(title: String): View {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val dot = TextView(context).apply {
            text = "✓"
            setTextColor(DSSColors.primary())
            textSize = 14f
            typeface = DSSFont.bold(context, 14f).typeface
        }
        row.addView(dot, LinearLayout.LayoutParams(20f.dpToPx(context), 20f.dpToPx(context)))
        val label = TextView(context).apply {
            text = title
            textSize = 14f
            typeface = DSSFont.regular(context, 14f).typeface
            setTextColor(DSSColors.textSecondary())
        }
        row.addView(
            label,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 12f.dpToPx(context) },
        )
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 4f.dpToPx(context); bottomMargin = 4f.dpToPx(context) }
        return row
    }

    private fun iconRow(item: DSSPlanCollectionView.CheckListItem): View {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val size = if (item.imageUrl != null) 28 else 24
        val iv = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(item.icon)
        }
        row.addView(iv, LinearLayout.LayoutParams(size.toFloat().dpToPx(context), size.toFloat().dpToPx(context)))
        DSSPlanCollectionView.loadImageInto(iv, item.imageUrl, item.icon)
        val label = TextView(context).apply {
            text = item.title
            textSize = 14f
            typeface = DSSFont.regular(context, 14f).typeface
            setTextColor(DSSColors.textSecondary())
        }
        row.addView(
            label,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 12f.dpToPx(context) },
        )
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 4f.dpToPx(context); bottomMargin = 4f.dpToPx(context) }
        return row
    }

    private fun toggle() {
        isExpanded = !isExpanded
        TransitionManager.beginDelayedTransition(this)
        expandableContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        toggleButton.text = if (isExpanded) "Ver menos" else "Mais detalhes"
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        cardBackground.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.surface(),
            cornerRadiusDp = 15f,
            strokeColor = DSSColors.borderDefault(),
            strokeWidthDp = 1f,
        )
        currentOfferLabel.setTextColor(DSSColors.textSecondary())
        planNameLabel.setTextColor(DSSColors.textPrimary())
        priceLabel.setTextColor(DSSColors.textPrimary())
        validityDateLabel.setTextColor(DSSColors.textSecondary())
        ilimitadosTitle.setTextColor(DSSColors.textPrimary())
        subscriptionsTitle.setTextColor(DSSColors.textPrimary())

        validityBadge.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.primary(),
            cornerRadiusDp = 12f,
        )
        validityBadge.setTextColor(DSSColors.textOnPrimary())

        toggleButton.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = android.graphics.Color.TRANSPARENT,
            cornerRadiusDp = 20f,
            strokeColor = DSSColors.borderDefault(),
            strokeWidthDp = 1f,
        )
        toggleButton.setTextColor(DSSColors.textSecondary())
    }
}
