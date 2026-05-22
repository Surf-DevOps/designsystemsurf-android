package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSConsumptionChartFlaListView` do iOS — lista horizontal da variante
 * FLA dos charts.
 */
class DSSConsumptionChartFlaListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    data class ItemConfig(
        val used: Int,
        val total: Int,
        val type: DSSConsumptionChartFla.TypeChart,
        val isInverted: Boolean = false,
        val action: (() -> Unit)?,
    )

    data class Style(
        val progressColor: Int = DSSColors.primary(),
        val trackColor: Int = Color.argb(77, 200, 200, 200),
        val usedTextColor: Int = Color.BLACK,
        val totalTextColor: Int = Color.DKGRAY,
    )

    private val items = mutableListOf<ItemConfig>()
    private var style: Style = Style()

    private val recyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        setBackgroundColor(Color.TRANSPARENT)
        clipToPadding = false
    }
    private val adapter = ChartAdapter()

    init {
        addView(
            recyclerView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        recyclerView.adapter = adapter
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { adapter.notifyDataSetChanged() }

    fun configure(items: List<ItemConfig>, style: Style = Style()) {
        this.items.clear()
        this.items.addAll(items)
        this.style = style
        adapter.notifyDataSetChanged()
    }

    private inner class ChartAdapter : RecyclerView.Adapter<DSSConsumptionChartFlaCell>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DSSConsumptionChartFlaCell {
            return DSSConsumptionChartFlaCell(parent.context).also {
                it.itemView.layoutParams = RecyclerView.LayoutParams(
                    200f.dpToPx(parent.context),
                    200f.dpToPx(parent.context),
                )
            }
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: DSSConsumptionChartFlaCell, position: Int) {
            val item = items[position]
            val lp = holder.itemView.layoutParams as RecyclerView.LayoutParams
            if (position < items.size - 1) lp.rightMargin = 50f.dpToPx(context)
            holder.itemView.layoutParams = lp
            holder.configure(
                used = item.used,
                total = item.total,
                type = item.type,
                isInverted = item.isInverted,
                progressColor = style.progressColor,
                trackColor = style.trackColor,
                usedTextColor = style.usedTextColor,
                totalTextColor = style.totalTextColor,
                action = item.action,
            )
        }
    }
}
