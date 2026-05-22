package com.surf.surfhubds.components

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
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
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSPlanAddonCollectionView.swift` (iOS) — lista de cards de adicionais (addons).
 *
 * Diferenças em relação a [DSSPlanCollectionView]:
 *  - addons sem `ratingGroups` não expandem, apenas selecionam visualmente
 *  - sem seção de canais de TV
 */
class DSSPlanAddonCollectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    data class AddonModel(
        val planId: String,
        val planName: String,
        val validityText: String,
        val priceCents: Int,
        val dataText: String,
        val parcelas: Int = 1,
        val hasRatingGroups: Boolean = true,
        val checkListItems: List<DSSPlanCollectionView.CheckListItem> = emptyList(),
        val unlimitedItems: List<DSSPlanCollectionView.CheckListItem> = emptyList(),
        val subscriptionItems: List<DSSPlanCollectionView.CheckListItem> = emptyList(),
    )

    interface Delegate {
        fun onSelectPlan(view: DSSPlanAddonCollectionView, plan: AddonModel, index: Int) {}
        fun onDeselectPlan(view: DSSPlanAddonCollectionView, plan: AddonModel, index: Int) {}
    }

    var delegate: Delegate? = null

    private val recycler = RecyclerView(context)
    private val adapter = AddonAdapter()
    private var showPlanNames: Boolean = true
    private var expandedIndex: Int = -1
    private var selectedIndex: Int = -1

    init {
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter
        addView(
            recycler,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
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

    fun setPlans(plans: List<AddonModel>, showPlanNames: Boolean = true) {
        this.showPlanNames = showPlanNames
        adapter.submit(plans)
    }

    fun getSelectedPlan(): AddonModel? {
        val idx = if (selectedIndex >= 0) selectedIndex else expandedIndex
        return adapter.itemAt(idx)
    }

    fun clearSelection() {
        if (selectedIndex >= 0) {
            adapter.itemAt(selectedIndex)?.let { delegate?.onDeselectPlan(this, it, selectedIndex) }
        }
        if (expandedIndex >= 0 && expandedIndex != selectedIndex) {
            adapter.itemAt(expandedIndex)?.let { delegate?.onDeselectPlan(this, it, expandedIndex) }
        }
        selectedIndex = -1
        expandedIndex = -1
        adapter.notifyDataSetChanged()
    }

    fun reloadData() = adapter.notifyDataSetChanged()

    private inner class AddonAdapter : RecyclerView.Adapter<AddonViewHolder>() {
        private val items = mutableListOf<AddonModel>()

        fun submit(list: List<AddonModel>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        fun itemAt(index: Int): AddonModel? = items.getOrNull(index)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddonViewHolder {
            val cell = AddonCardCell(parent.context)
            cell.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT,
            )
            return AddonViewHolder(cell)
        }

        override fun onBindViewHolder(holder: AddonViewHolder, position: Int) {
            val plan = items[position]
            holder.cell.configure(plan, showPlanNames)
            val isExpanded = position == expandedIndex && plan.hasRatingGroups
            val isSelectedOnly = position == selectedIndex && !plan.hasRatingGroups
            holder.cell.setExpanded(isExpanded, animated = false)
            holder.cell.setSelectedStyle(isExpanded || isSelectedOnly)

            holder.cell.setOnClickListener {
                if (!plan.hasRatingGroups) {
                    // Toggle simple selection
                    val previous = selectedIndex
                    selectedIndex = position
                    if (previous >= 0 && previous != position) notifyItemChanged(previous)
                    notifyItemChanged(position)
                    delegate?.onSelectPlan(this@DSSPlanAddonCollectionView, plan, position)
                    return@setOnClickListener
                }
                val previouslyExpanded = expandedIndex
                val expanding = position != expandedIndex
                selectedIndex = -1
                expandedIndex = if (expanding) position else -1
                if (previouslyExpanded >= 0 && previouslyExpanded != position) {
                    notifyItemChanged(previouslyExpanded)
                }
                notifyItemChanged(position)
                if (expanding) delegate?.onSelectPlan(this@DSSPlanAddonCollectionView, plan, position)
                else delegate?.onDeselectPlan(this@DSSPlanAddonCollectionView, plan, position)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private class AddonViewHolder(val cell: AddonCardCell) : RecyclerView.ViewHolder(cell)

    private class AddonCardCell(context: Context) : FrameLayout(context), ThemeAware {

        private val container = FrameLayout(context)
        private val mainColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        private val headerContainer = FrameLayout(context)
        private val validityLabel = TextView(context)
        private val priceLabel = TextView(context)
        private val planNameLabel = TextView(context)
        private val downArrow = TextView(context)
        private val untilLabel = TextView(context)
        private val dataLabel = TextView(context)

        private val expandableContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        private val benefitsStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        private val separator = View(context)
        private val ilimitadosTitle = TextView(context)
        private val ilimitadosStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        private val assinaturasTitle = TextView(context)
        private val assinaturasStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        private var isExpanded = false
        private var selectedStyle = false

        init {
            val pad = 16f.dpToPx(context)
            setPadding(pad, 8f.dpToPx(context), pad, 8f.dpToPx(context))
            container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            mainColumn.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            container.addView(mainColumn)
            addView(container)

            setupHeader()
            setupExpandable()
            refresh()
            setupThemeObserver()
        }

        private fun setupHeader() {
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
            downArrow.apply { text = "v"; textSize = 14f; gravity = Gravity.CENTER }
            untilLabel.apply {
                text = "Até"; textSize = 18f; typeface = DSSFont.light(context, 18f).typeface
            }
            dataLabel.apply { textSize = 22f; typeface = DSSFont.bold(context, 22f).typeface }

            headerContainer.addView(
                validityLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.START or Gravity.TOP
                    leftMargin = 16f.dpToPx(context); topMargin = 16f.dpToPx(context)
                },
            )
            headerContainer.addView(
                downArrow,
                LayoutParams(20f.dpToPx(context), 20f.dpToPx(context)).apply {
                    gravity = Gravity.END or Gravity.TOP
                    topMargin = 16f.dpToPx(context); rightMargin = 16f.dpToPx(context)
                },
            )
            headerContainer.addView(
                priceLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.END or Gravity.TOP
                    topMargin = 16f.dpToPx(context); rightMargin = 44f.dpToPx(context)
                },
            )
            headerContainer.addView(
                untilLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = 16f.dpToPx(context); topMargin = 44f.dpToPx(context)
                },
            )
            headerContainer.addView(
                dataLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    leftMargin = 16f.dpToPx(context); topMargin = 72f.dpToPx(context); bottomMargin = 16f.dpToPx(context)
                },
            )
            headerContainer.addView(
                planNameLabel,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.END
                    topMargin = 80f.dpToPx(context); rightMargin = 16f.dpToPx(context)
                },
            )
            headerContainer.minimumHeight = 130f.dpToPx(context)
            mainColumn.addView(headerContainer)
        }

        private fun setupExpandable() {
            expandableContainer.visibility = View.GONE
            val pad = 16f.dpToPx(context)
            expandableContainer.setPadding(pad, pad, pad, pad)

            expandableContainer.addView(benefitsStack)
            separator.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f.dpToPx(context),
            ).apply { topMargin = 16f.dpToPx(context); bottomMargin = 16f.dpToPx(context) }
            expandableContainer.addView(separator)

            ilimitadosTitle.apply { text = "Ilimitados"; textSize = 14f; typeface = DSSFont.bold(context, 14f).typeface }
            expandableContainer.addView(ilimitadosTitle)
            expandableContainer.addView(
                ilimitadosStack,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 8f.dpToPx(context) },
            )

            assinaturasTitle.apply { text = "Assinaturas"; textSize = 14f; typeface = DSSFont.bold(context, 14f).typeface }
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
            mainColumn.addView(expandableContainer)
        }

        fun configure(plan: AddonModel, showPlanName: Boolean) {
            validityLabel.text = plan.validityText
            val displayPrice = if (plan.parcelas > 1) plan.priceCents / plan.parcelas else plan.priceCents
            priceLabel.text = if (plan.parcelas > 1) {
                "${plan.parcelas}x R$ ${DSSPlanCollectionView.formatPrice(displayPrice)}"
            } else {
                "R$ ${DSSPlanCollectionView.formatPrice(displayPrice)}"
            }
            dataLabel.text = plan.dataText
            planNameLabel.text = plan.planName
            planNameLabel.visibility = if (showPlanName) View.VISIBLE else View.GONE
            downArrow.visibility = if (plan.hasRatingGroups) View.VISIBLE else View.GONE

            benefitsStack.removeAllViews()
            for (item in plan.checkListItems) {
                benefitsStack.addView(createItemView(item, isCheckmark = true))
            }
            configureSection(ilimitadosStack, ilimitadosTitle, plan.unlimitedItems)
            configureSection(assinaturasStack, assinaturasTitle, plan.subscriptionItems)
        }

        private fun configureSection(stack: LinearLayout, title: TextView, items: List<DSSPlanCollectionView.CheckListItem>) {
            stack.removeAllViews()
            val hasItems = items.isNotEmpty()
            title.visibility = if (hasItems) View.VISIBLE else View.GONE
            stack.visibility = if (hasItems) View.VISIBLE else View.GONE
            if (!hasItems) return
            for (item in items) stack.addView(createItemView(item, isCheckmark = false))
        }

        private fun createItemView(item: DSSPlanCollectionView.CheckListItem, isCheckmark: Boolean): View {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val size = if (item.imageUrl != null) 28 else 20

            val iv = ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageDrawable(item.icon)
                if (isCheckmark) setColorFilter(DSSColors.primary(), PorterDuff.Mode.SRC_IN)
                else clearColorFilter()
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
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { leftMargin = 12f.dpToPx(context) },
            )
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 4f.dpToPx(context); bottomMargin = 4f.dpToPx(context) }
            return row
        }

        fun setExpanded(expanded: Boolean, animated: Boolean) {
            if (isExpanded == expanded) return
            isExpanded = expanded
            expandableContainer.visibility = if (expanded) View.VISIBLE else View.GONE
            downArrow.rotation = if (expanded) 180f else 0f
        }

        fun setSelectedStyle(isSelected: Boolean) {
            selectedStyle = isSelected
            refresh()
        }

        override fun applyTheme(theme: Theme) { refresh() }

        private fun refresh() {
            val bg = DSSColors.surface()
            val stroke = if (selectedStyle) DSSColors.primary() else DSSColors.borderDefault()
            val width = if (selectedStyle) 2f else 1f
            container.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = bg,
                cornerRadiusDp = 16f,
                strokeColor = stroke,
                strokeWidthDp = width,
            )
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
        }
    }
}
