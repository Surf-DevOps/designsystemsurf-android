package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSConsumptionChartFlaCell` do iOS — célula que hospeda um [DSSConsumptionChartFla].
 */
class DSSConsumptionChartFlaCell(parent: Context) : RecyclerView.ViewHolder(FrameLayout(parent)) {

    private val container = itemView as FrameLayout
    private var chartView: DSSConsumptionChartFla? = null

    init {
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    fun configure(
        used: Int,
        total: Int,
        type: DSSConsumptionChartFla.TypeChart,
        isInverted: Boolean,
        progressColor: Int = com.surf.surfhubds.theme.DSSColors.primary(),
        trackColor: Int = Color.argb(77, 200, 200, 200),
        usedTextColor: Int = Color.BLACK,
        totalTextColor: Int = Color.DKGRAY,
        action: (() -> Unit)?,
    ) {
        container.removeAllViews()
        val chart = DSSConsumptionChartFla(container.context).apply {
            this.used = used.toDouble()
            this.total = total.toDouble()
            this.type = type
            this.isInverted = isInverted
            this.progressColor = progressColor
            this.trackColor = trackColor
            this.usedTextColor = usedTextColor
            this.chartAction = action
        }
        val size = 200f.dpToPx(container.context)
        val lp = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
        container.addView(chart, lp)
        chartView = chart
    }
}
