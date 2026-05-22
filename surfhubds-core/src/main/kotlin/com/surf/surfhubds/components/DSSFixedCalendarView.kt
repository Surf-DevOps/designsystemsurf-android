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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * Port do `DSSFixedCalendarView.swift` (iOS) — calendário fixo (sem navegação de mês)
 * que destaca um intervalo selecionável.
 */
class DSSFixedCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    var selectedDate: Date? = null
        private set

    private var maxDate: Date? = null
    private var minDate: Date? = null
    private var monthDate: Date? = null
    private val enabledDays = HashSet<Int>()
    private val calendar: Calendar = Calendar.getInstance()

    private val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val monthLabel = TextView(context)
    private val weekdayRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val daysGrid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))

    init {
        column.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(column)

        monthLabel.apply {
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = DSSFont.regular(context, 16f).typeface
        }
        column.addView(monthLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        column.addView(
            weekdayRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 16f.dpToPx(context) },
        )
        column.addView(
            daysGrid,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 8f.dpToPx(context) },
        )

        setupWeekdayHeaders()
        refresh()
        setupThemeObserver()
    }

    private fun setupWeekdayHeaders() {
        weekdayRow.removeAllViews()
        weekdayRow.weightSum = 7f
        val days = listOf("D", "S", "T", "Q", "Q", "S", "S")
        for (d in days) {
            val l = TextView(context).apply {
                text = d
                textSize = 14f
                typeface = DSSFont.bold(context, 14f).typeface
                gravity = Gravity.CENTER
                setTextColor(DSSColors.textPrimary())
            }
            weekdayRow.addView(l, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    /** Configura com a data máxima (selecionável) e quantos dias para trás são habilitados. */
    fun configure(maxDate: Date, daysBack: Int) {
        this.maxDate = maxDate
        val cal = Calendar.getInstance().apply { time = maxDate }
        cal.add(Calendar.DAY_OF_MONTH, -daysBack)
        this.minDate = cal.time
        this.selectedDate = maxDate
        this.monthDate = maxDate

        computeEnabledDays()
        var title = monthFormatter.format(maxDate)
        title = title.replaceFirstChar { it.uppercase() }
        monthLabel.text = title
        buildDaysGrid()
    }

    private fun computeEnabledDays() {
        enabledDays.clear()
        val max = maxDate ?: return
        val min = minDate ?: return
        val maxCal = Calendar.getInstance().apply { time = max }
        val minCal = Calendar.getInstance().apply { time = min }

        val maxDay = maxCal.get(Calendar.DAY_OF_MONTH)
        val maxMonth = maxCal.get(Calendar.MONTH)
        val maxYear = maxCal.get(Calendar.YEAR)
        val minDay = minCal.get(Calendar.DAY_OF_MONTH)
        val minMonth = minCal.get(Calendar.MONTH)
        val minYear = minCal.get(Calendar.YEAR)

        if (minMonth == maxMonth && minYear == maxYear) {
            for (d in minDay..maxDay) enabledDays.add(d)
        } else {
            for (d in 1..maxDay) enabledDays.add(d)
        }
    }

    private fun buildDaysGrid() {
        daysGrid.removeAllViews()
        val month = monthDate ?: return
        calendar.time = month
        val year = calendar.get(Calendar.YEAR)
        val monthIndex = calendar.get(Calendar.MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val firstCal = Calendar.getInstance().apply { set(year, monthIndex, 1) }
        val firstWeekday = firstCal.get(Calendar.DAY_OF_WEEK) // 1 = Sun
        val offset = firstWeekday - 1
        val totalCells = offset + daysInMonth
        val rows = ceil(totalCells / 7.0).toInt()

        var dayNumber = 1
        for (row in 0 until rows) {
            val rowStack = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 7f
            }
            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val cell: View
                if (cellIndex < offset || dayNumber > daysInMonth) {
                    cell = View(context)
                } else {
                    val day = dayNumber
                    val isEnabled = enabledDays.contains(day)
                    val isSelected = selectedDate?.let {
                        val c = Calendar.getInstance().apply { time = it }
                        c.get(Calendar.DAY_OF_MONTH) == day &&
                            c.get(Calendar.MONTH) == monthIndex
                    } ?: false
                    cell = makeDayCell(day, isEnabled, isSelected)
                    dayNumber++
                }
                val lp = LinearLayout.LayoutParams(0, 40f.dpToPx(context), 1f)
                rowStack.addView(cell, lp)
            }
            daysGrid.addView(
                rowStack,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
    }

    private fun makeDayCell(day: Int, enabled: Boolean, selected: Boolean): View {
        val frame = FrameLayout(context)
        val label = TextView(context).apply {
            text = day.toString()
            textSize = 15f
            typeface = DSSFont.regular(context, 15f).typeface
            gravity = Gravity.CENTER
        }
        when {
            selected -> {
                label.setTextColor(DSSColors.textOnPrimary())
                label.background = DrawableFactory.rounded(
                    context = context,
                    backgroundColor = DSSColors.primary(),
                    cornerRadiusDp = 20f,
                )
            }
            enabled -> {
                label.setTextColor(DSSColors.textPrimary())
            }
            else -> {
                label.setTextColor(DSSColors.textTertiary())
            }
        }
        val lp = LayoutParams(36f.dpToPx(context), 36f.dpToPx(context)).apply {
            gravity = Gravity.CENTER
        }
        frame.addView(label, lp)

        if (enabled) {
            frame.isClickable = true
            frame.setOnClickListener {
                val c = Calendar.getInstance().apply { time = monthDate ?: return@setOnClickListener }
                c.set(Calendar.DAY_OF_MONTH, day)
                selectedDate = c.time
                buildDaysGrid()
            }
        }
        return frame
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        monthLabel.setTextColor(DSSColors.textPrimary())
        for (i in 0 until weekdayRow.childCount) {
            (weekdayRow.getChildAt(i) as? TextView)?.setTextColor(DSSColors.textPrimary())
        }
        if (monthDate != null) buildDaysGrid()
    }
}
