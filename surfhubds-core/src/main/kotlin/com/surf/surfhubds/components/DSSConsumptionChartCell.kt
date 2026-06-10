package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSConsumptionChartCell` do iOS — célula que hospeda um [DSSConsumptionChart].
 */
class DSSConsumptionChartCell(parent: Context) : RecyclerView.ViewHolder(FrameLayout(parent)) {

    private val container = itemView as FrameLayout
    private var chartView: DSSConsumptionChart? = null

    init {
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    fun configure(
        used: Int,
        total: Int,
        type: DSSConsumptionChart.TypeChart,
        isInverted: Boolean,
        progressColor: Int = com.surf.surfhubds.theme.DSSColors.primary(),
        // iOS default: UIColor.lightGray.withAlphaComponent(0.3) -> ~170,170,170 @ alpha 77.
        trackColor: Int = Color.argb(77, 170, 170, 170),
        // iOS defaults: usedFont = .boldSystemFont(ofSize: 26), totalFont = .systemFont(ofSize: 14).
        usedFontSizeSp: Float = 26f,
        totalFontSizeSp: Float = 14f,
        usedTextColor: Int = Color.BLACK,
        totalTextColor: Int = Color.DKGRAY,
        usedTypeface: Typeface? = null,
        totalTypeface: Typeface? = null,
        action: (() -> Unit)?,
    ) {
        container.removeAllViews()
        val chart = DSSConsumptionChart(container.context).apply {
            this.used = used
            this.total = total
            this.type = type
            this.isInverted = isInverted
            this.progressColor = progressColor
            this.trackColor = trackColor
            this.usedFontSizeSp = usedFontSizeSp
            this.totalFontSizeSp = totalFontSizeSp
            this.usedTextColor = usedTextColor
            this.totalTextColor = totalTextColor
            this.chartAction = action
        }
        val size = 200f.dpToPx(container.context)
        val lp = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
        container.addView(chart, lp)
        chartView = chart
    }
}
