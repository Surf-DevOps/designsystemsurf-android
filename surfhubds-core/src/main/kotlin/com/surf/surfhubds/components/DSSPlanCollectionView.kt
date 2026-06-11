package com.surf.surfhubds.components

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.Utility
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dpToPxFloat
import java.net.URL
import java.util.concurrent.Executors

/**
 * Port do `DSSPlanCollectionView.swift` (iOS) — lista vertical de cards de plano expansíveis.
 *
 * Cada card mostra header (validade + preço + "Até X GB") e, ao tocar, expande
 * para uma seção com checkmarks, "Ilimitados", "Assinaturas" e canais.
 */
class DSSPlanCollectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Item simples para listas de checagem/ícones nas seções expandidas. */
    data class CheckListItem(
        val title: String,
        val icon: Drawable?,
        val imageUrl: String? = null,
    )

    /** Modelo agnóstico de plano para o adapter (não depende do SurfAPIKit). */
    data class PlanModel(
        val planId: String,
        val planName: String,
        val validityText: String,
        /** Preço total em centavos (será dividido por parcelas). */
        val priceCents: Int,
        val dataText: String,
        val parcelas: Int = 1,
        val checkListItems: List<CheckListItem> = emptyList(),
        val unlimitedItems: List<CheckListItem> = emptyList(),
        val subscriptionItems: List<CheckListItem> = emptyList(),
        val packageInfo: PackageInfo? = null,
        val packageType: String? = null,
    )

    data class PackageInfo(val name: String, val imageUrl: String?)

    interface Delegate {
        fun onSelectPlan(view: DSSPlanCollectionView, plan: PlanModel, index: Int) {}
        fun onDeselectPlan(view: DSSPlanCollectionView, plan: PlanModel, index: Int) {}
    }

    var delegate: Delegate? = null

    private val recycler = RecyclerView(context)
    private val adapter = PlanAdapter()
    private var showPlanNames: Boolean = true
    private var expandedIndex: Int = -1

    /**
     * Quando true, exibe a faixa "Oferta atual" no card que casar com o plano vigente.
     * Espelha `showsCurrentOffer` do iOS — recarrega ao mudar.
     */
    var showsCurrentOffer: Boolean = false
        set(value) {
            field = value
            adapter.notifyDataSetChanged()
        }

    /** `planId` (noPlano) do plano vigente, comparado com os planos da lista. */
    private var currentPlanId: String? = null

    init {
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter
        recycler.clipToPadding = false
        addView(
            recycler,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) {
        refresh()
        adapter.notifyDataSetChanged()
    }

    private fun refresh() {
        setBackgroundColor(DSSColors.background())
        recycler.setBackgroundColor(DSSColors.background())
    }

    /** Substitui a lista exibida e recarrega. */
    fun setPlans(plans: List<PlanModel>, showPlanNames: Boolean = true) {
        this.showPlanNames = showPlanNames
        adapter.submit(plans)
    }

    /** Retorna o plano expandido/selecionado, se houver. */
    fun getSelectedPlan(): PlanModel? = adapter.itemAt(expandedIndex)

    /**
     * Define o plano vigente (oferta atual) pelo `planId` (equivalente ao `noPlano` do iOS).
     * A faixa só aparece se [showsCurrentOffer] for true.
     */
    fun setCurrentPlan(planId: String?) {
        currentPlanId = planId
        adapter.notifyDataSetChanged()
    }

    /** Indica se o plano informado é o vigente (oferta atual): casa por `planId`. */
    private fun isCurrentOffer(plan: PlanModel): Boolean {
        if (!showsCurrentOffer) return false
        val current = currentPlanId ?: return false
        return current == plan.planId
    }

    /** Limpa a seleção atual. */
    fun clearSelection() {
        val previous = expandedIndex
        if (previous >= 0) {
            adapter.itemAt(previous)?.let { delegate?.onDeselectPlan(this, it, previous) }
        }
        expandedIndex = -1
        adapter.notifyDataSetChanged()
    }

    fun reloadData() = adapter.notifyDataSetChanged()

    // region Adapter / ViewHolder

    private inner class PlanAdapter : RecyclerView.Adapter<PlanViewHolder>() {
        private val items = mutableListOf<PlanModel>()

        fun submit(list: List<PlanModel>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        fun itemAt(index: Int): PlanModel? = items.getOrNull(index)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
            val cell = PlanCardCell(parent.context)
            cell.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT,
            )
            return PlanViewHolder(cell)
        }

        override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
            val plan = items[position]
            holder.cell.configure(
                plan = plan,
                showPlanName = showPlanNames,
            )
            val isExpanded = position == expandedIndex
            holder.cell.setExpanded(isExpanded, animated = false)
            holder.cell.setSelectedStyle(isExpanded)
            holder.cell.setCurrentOffer(isCurrentOffer(plan))
            holder.cell.setOnClickListener {
                val previouslyExpanded = expandedIndex
                val expanding = position != expandedIndex
                expandedIndex = if (expanding) position else -1

                if (previouslyExpanded >= 0 && previouslyExpanded != position) {
                    notifyItemChanged(previouslyExpanded)
                }
                notifyItemChanged(position)

                if (expanding) {
                    delegate?.onSelectPlan(this@DSSPlanCollectionView, plan, position)
                } else {
                    delegate?.onDeselectPlan(this@DSSPlanCollectionView, plan, position)
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private class PlanViewHolder(val cell: PlanCardCell) : RecyclerView.ViewHolder(cell)

    // endregion

    /**
     * Cell visual do card de plano (equivalente ao DSSSetPlanCardCollectionViewCell em iOS).
     * Construído programaticamente, totalmente sem XML.
     */
    private class PlanCardCell(context: Context) : FrameLayout(context), ThemeAware {

        private val container = FrameLayout(context)
        private val mainColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        // Faixa "Oferta atual" (fica acima/atrás do card; o card cobre a parte inferior).
        private val currentOfferBadge = TextView(context)
        private var currentOfferShown = false

        // Header
        private val headerContainer = FrameLayout(context)
        private val validityLabel = TextView(context)
        private val priceLabel = TextView(context)
        private val planNameLabel = TextView(context)
        private val downArrow = TextView(context)
        private val untilLabel = TextView(context)
        private val dataLabel = TextView(context)

        // Expandable
        private val expandableContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        private val benefitsStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        private val separator = View(context)
        private val ilimitadosTitle = TextView(context)
        private val ilimitadosStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        private val assinaturasTitle = TextView(context)
        private val assinaturasStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        private val channelsView = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        private val channelsImage = ImageView(context)
        private val channelsLabel = TextView(context)

        private var isExpanded = false
        private var selectedStyle = false

        init {
            setupContainer()
            setupHeader()
            setupExpandable()
            refresh()
            setupThemeObserver()
        }

        private fun setupContainer() {
            // Outer padding
            val pad = 16f.dpToPx(context)
            setPadding(pad, 8f.dpToPx(context), pad, 8f.dpToPx(context))

            // Faixa "Oferta atual" — adicionada ANTES do container para ficar atrás dele.
            currentOfferBadge.apply {
                text = "Oferta atual"
                textSize = 14f
                typeface = DSSFont.medium(context, 14f).typeface
                setTextColor(android.graphics.Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(
                    20f.dpToPx(context),
                    6f.dpToPx(context),
                    20f.dpToPx(context),
                    14f.dpToPx(context),
                )
                visibility = View.GONE
            }
            addView(
                currentOfferBadge,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    // iOS: top = contentView.top; trailing = contentView.trailing - 16.
                    // O padding horizontal do cell (16) já posiciona a borda direita.
                    gravity = Gravity.TOP or Gravity.END
                },
            )

            container.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            )
            mainColumn.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            )
            container.addView(mainColumn)
            addView(container)
        }

        /**
         * Mostra/esconde a faixa "Oferta atual" acima do card.
         * iOS empurra o card 20pt para baixo (top 8 -> 28) para a faixa aparecer atrás dele.
         */
        fun setCurrentOffer(show: Boolean) {
            currentOfferShown = show
            currentOfferBadge.visibility = if (show) View.VISIBLE else View.GONE
            val lp = container.layoutParams as? LayoutParams ?: return
            lp.topMargin = if (show) CURRENT_OFFER_EXTRA_HEIGHT.dpToPx(context) else 0
            container.layoutParams = lp
            refreshCurrentOfferBackground()
        }

        private fun refreshCurrentOfferBackground() {
            val color = DSSColors.primary()
            val r = 12f.dpToPxFloat(context)
            currentOfferBadge.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(color)
                // Apenas cantos superiores (iOS maskedCorners = topLeft|topRight).
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
        }

        private fun setupHeader() {
            headerContainer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )

            validityLabel.apply {
                textSize = 12f
                typeface = DSSFont.light(context, 12f).typeface
                setPadding(8f.dpToPx(context), 4f.dpToPx(context), 8f.dpToPx(context), 4f.dpToPx(context))
            }
            priceLabel.apply {
                textSize = 20f
                typeface = DSSFont.bold(context, 20f).typeface
                gravity = Gravity.END
            }
            planNameLabel.apply {
                textSize = 14f
                typeface = DSSFont.regular(context, 14f).typeface
                gravity = Gravity.END
            }
            downArrow.apply {
                text = "v"
                textSize = 14f
                gravity = Gravity.CENTER
            }
            untilLabel.apply {
                text = "Até"
                textSize = 18f
                typeface = DSSFont.light(context, 18f).typeface
            }
            dataLabel.apply {
                textSize = 22f
                typeface = DSSFont.bold(context, 22f).typeface
            }

            headerContainer.addView(
                validityLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.START or Gravity.TOP
                    leftMargin = 16f.dpToPx(context)
                    topMargin = 16f.dpToPx(context)
                },
            )
            headerContainer.addView(
                downArrow,
                LayoutParams(20f.dpToPx(context), 20f.dpToPx(context)).apply {
                    gravity = Gravity.END or Gravity.TOP
                    topMargin = 16f.dpToPx(context)
                    rightMargin = 16f.dpToPx(context)
                },
            )
            headerContainer.addView(
                priceLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.END or Gravity.TOP
                    topMargin = 16f.dpToPx(context)
                    rightMargin = 44f.dpToPx(context)
                },
            )
            headerContainer.addView(
                untilLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.START
                    leftMargin = 16f.dpToPx(context)
                    topMargin = 44f.dpToPx(context)
                },
            )
            headerContainer.addView(
                dataLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.START
                    leftMargin = 16f.dpToPx(context)
                    topMargin = 72f.dpToPx(context)
                    bottomMargin = 16f.dpToPx(context)
                },
            )
            headerContainer.addView(
                planNameLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.END
                    topMargin = 80f.dpToPx(context)
                    rightMargin = 16f.dpToPx(context)
                },
            )
            headerContainer.minimumHeight = 130f.dpToPx(context)

            mainColumn.addView(headerContainer)
        }

        private fun setupExpandable() {
            expandableContainer.visibility = View.GONE
            val pad = 16f.dpToPx(context)
            expandableContainer.setPadding(pad, pad, pad, pad)

            benefitsStack.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            expandableContainer.addView(benefitsStack)

            separator.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f.dpToPx(context),
            ).apply { topMargin = 16f.dpToPx(context); bottomMargin = 16f.dpToPx(context) }
            expandableContainer.addView(separator)

            ilimitadosTitle.apply {
                text = "Ilimitados"
                textSize = 14f
                typeface = DSSFont.bold(context, 14f).typeface
                maxLines = 1 // iOS makeSectionTitleLabel numberOfLines = 1
            }
            expandableContainer.addView(
                ilimitadosTitle,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
            expandableContainer.addView(
                ilimitadosStack,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 8f.dpToPx(context) },
            )

            assinaturasTitle.apply {
                text = "Assinaturas"
                textSize = 14f
                typeface = DSSFont.bold(context, 14f).typeface
                maxLines = 1 // iOS makeSectionTitleLabel numberOfLines = 1
            }
            expandableContainer.addView(
                assinaturasTitle,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 16f.dpToPx(context) },
            )
            expandableContainer.addView(
                assinaturasStack,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 8f.dpToPx(context) },
            )

            channelsView.gravity = Gravity.CENTER_VERTICAL
            channelsImage.layoutParams = LinearLayout.LayoutParams(
                200f.dpToPx(context),
                33f.dpToPx(context),
            )
            channelsLabel.apply {
                textSize = 14f
                typeface = DSSFont.medium(context, 14f).typeface
            }
            channelsView.addView(channelsImage)
            channelsView.addView(
                channelsLabel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { leftMargin = 12f.dpToPx(context) },
            )
            channelsView.visibility = View.GONE
            expandableContainer.addView(
                channelsView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 16f.dpToPx(context) },
            )

            mainColumn.addView(expandableContainer)
        }

        fun configure(plan: PlanModel, showPlanName: Boolean) {
            validityLabel.text = plan.validityText
            val displayPrice = if (plan.parcelas > 1) plan.priceCents / plan.parcelas else plan.priceCents
            // iOS: parcelas>1 -> "Nx R$<valor>" (sem espaço); parcelas==1 -> "R$ <valor>" (com espaço).
            priceLabel.text = if (plan.parcelas > 1) {
                "${plan.parcelas}x R$${formatPrice(displayPrice)}"
            } else {
                "R$ ${formatPrice(displayPrice)}"
            }
            dataLabel.text = plan.dataText

            planNameLabel.text = plan.planName
            planNameLabel.visibility = if (showPlanName) View.VISIBLE else View.GONE

            benefitsStack.removeAllViews()
            // iOS benefitsStackView.spacing = 6.
            plan.checkListItems.forEachIndexed { index, item ->
                val spacing = if (index == 0) 0f else 6f
                benefitsStack.addView(createItemView(item, isCheckmark = true, topSpacingDp = spacing))
            }

            configureSection(ilimitadosStack, ilimitadosTitle, plan.unlimitedItems)

            val subs = buildList {
                plan.packageInfo?.let {
                    add(CheckListItem(it.name, null, it.imageUrl))
                }
                addAll(plan.subscriptionItems)
            }
            configureSection(assinaturasStack, assinaturasTitle, subs)

            configureChannelsSection(plan.packageType)

            // iOS: garante estado inicial colapsado após configurar.
            isExpanded = false
            expandableContainer.visibility = View.GONE
            downArrow.animate().cancel()
            downArrow.rotation = 0f
        }

        private fun configureSection(stack: LinearLayout, title: TextView, items: List<CheckListItem>) {
            stack.removeAllViews()
            val hasItems = items.isNotEmpty()
            title.visibility = if (hasItems) View.VISIBLE else View.GONE
            stack.visibility = if (hasItems) View.VISIBLE else View.GONE
            if (!hasItems) return
            // iOS makeSectionItemsStack().spacing = 12.
            items.forEachIndexed { index, item ->
                val spacing = if (index == 0) 0f else 12f
                stack.addView(createItemView(item, isCheckmark = false, topSpacingDp = spacing))
            }
        }

        private fun configureChannelsSection(packageType: String?) {
            val type = packageType?.uppercase()
            if (type == null || (!type.contains("ESSENCIAL") && !type.contains("TOTAL"))) {
                channelsView.visibility = View.GONE
                return
            }
            channelsView.visibility = View.VISIBLE
            val essencial = type.contains("ESSENCIAL")
            channelsLabel.text = if (essencial) "+5 canais" else "+17 canais"

            // iOS: ImageLoader.image(named: "channels_essencial"/"channels_total").
            val imageName = if (essencial) "channels_essencial" else "channels_total"
            val drawable = ImageLoader.image(context, imageName)
            if (drawable != null) {
                channelsImage.setImageDrawable(drawable)
                channelsImage.background = null
            } else {
                // Placeholder: fundo cinza com canto 4 (iOS systemGray5 + cornerRadius 4).
                channelsImage.setImageDrawable(null)
                channelsImage.background = DrawableFactory.rounded(
                    context = context,
                    backgroundColor = DSSColors.backgroundSecondary(),
                    cornerRadiusDp = 4f,
                )
            }
        }

        /**
         * @param topSpacingDp espaçamento (dp) acima do item — espelha o `spacing` do UIStackView do iOS
         *   (benefits = 6, seções Ilimitados/Assinaturas = 12). 0 para o primeiro item.
         */
        private fun createItemView(item: CheckListItem, isCheckmark: Boolean, topSpacingDp: Float): View {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val size = if (item.imageUrl != null) 28 else 20

            val iv = ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(item.icon)
                if (isCheckmark) {
                    setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
                } else {
                    clearColorFilter()
                }
            }
            row.addView(
                iv,
                LinearLayout.LayoutParams(size.toFloat().dpToPx(context), size.toFloat().dpToPx(context)),
            )
            loadImageInto(iv, item.imageUrl, item.icon)

            val label = TextView(context).apply {
                text = item.title
                textSize = 14f
                typeface = DSSFont.regular(context, 14f).typeface
                setTextColor(DSSColors.textSecondary())
                // iOS: numberOfLines = 0 (multilinha).
                maxLines = Integer.MAX_VALUE
            }
            row.addView(
                label,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply { leftMargin = 12f.dpToPx(context) },
            )

            // iOS: o espaçamento entre itens vem do UIStackView (sem margem no primeiro item).
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = topSpacingDp.dpToPx(context) }
            row.layoutParams = lp
            return row
        }

        fun setExpanded(expanded: Boolean, animated: Boolean) {
            if (isExpanded == expanded) return
            isExpanded = expanded
            // iOS: conteúdo aparece/some instantaneamente; só o chevron anima.
            expandableContainer.visibility = if (expanded) View.VISIBLE else View.GONE
            val target = if (expanded) 180f else 0f
            if (animated) {
                // iOS: UIView.animate 0.3s easeInOut girando o chevron.
                downArrow.animate().rotation(target).setDuration(300L).start()
            } else {
                downArrow.animate().cancel()
                downArrow.rotation = target
            }
        }

        fun setSelectedStyle(isSelected: Boolean) {
            selectedStyle = isSelected
            refresh()
        }

        override fun applyTheme(theme: Theme) { refresh() }

        private fun refresh() {
            val bg = DSSColors.surface()
            val stroke = if (selectedStyle) DSSColors.primary() else DSSColors.borderDefault()
            val strokeWidth = if (selectedStyle) 2f else 1f
            container.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = bg,
                cornerRadiusDp = 16f,
                strokeColor = stroke,
                strokeWidthDp = strokeWidth,
            )
            // iOS: shadow opacity 0.1, offset (0,2), radius 4. Aproximação via elevation.
            container.elevation = 4f.dpToPxFloat(context)

            validityLabel.setTextColor(DSSColors.textPrimary())
            validityLabel.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = ColorUtils.setAlphaComponent(DSSColors.primary(), 26),
                cornerRadiusDp = 10f,
            )
            priceLabel.setTextColor(DSSColors.textPrimary())
            planNameLabel.setTextColor(DSSColors.textPrimary())
            untilLabel.setTextColor(DSSColors.textPrimary())
            dataLabel.setTextColor(DSSColors.primary())
            downArrow.setTextColor(DSSColors.textPrimary())
            separator.setBackgroundColor(DSSColors.divider())
            ilimitadosTitle.setTextColor(DSSColors.textPrimary())
            assinaturasTitle.setTextColor(DSSColors.textPrimary())
            channelsLabel.setTextColor(DSSColors.primary())
            refreshCurrentOfferBackground()
        }
    }

    companion object {
        /** Altura extra (dp) reservada para a faixa "Oferta atual" (iOS currentOfferExtraHeight = 20). */
        internal const val CURRENT_OFFER_EXTRA_HEIGHT = 20f

        // Espelha `Utility.formatPrice` do iOS: NumberFormatter .decimal pt_BR
        // (separador de milhar "." e decimal ","), 2 casas. Ex.: 1234567 -> "12.345,67".
        internal fun formatPrice(cents: Int): String = Utility.formatPrice(cents)

        private val ioExecutor = Executors.newCachedThreadPool()

        internal fun loadImageInto(imageView: ImageView, url: String?, fallback: Drawable?) {
            if (url == null) {
                imageView.setImageDrawable(fallback)
                return
            }
            if (url.startsWith("data:image")) {
                val base64 = url
                    .substringAfter(",", url)
                if (base64.isNotEmpty()) {
                    try {
                        val data = Base64.decode(base64, Base64.DEFAULT)
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
                        if (bmp != null) {
                            imageView.setImageBitmap(bmp)
                            return
                        }
                    } catch (_: Throwable) { }
                }
                imageView.setImageDrawable(fallback)
                return
            }

            imageView.setImageDrawable(fallback)
            ioExecutor.execute {
                try {
                    val stream = URL(url).openStream()
                    val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                    stream.close()
                    if (bmp != null) {
                        imageView.post { imageView.setImageBitmap(bmp) }
                    }
                } catch (_: Throwable) { }
            }
        }
    }
}
