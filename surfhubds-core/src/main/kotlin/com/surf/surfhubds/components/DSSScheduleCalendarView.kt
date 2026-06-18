package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
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
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

/**
 * Port do `DSSScheduleCalendarView.swift` (iOS) — calendário com navegação de mês,
 * range mínimo/máximo e label descritiva da data selecionada.
 */
class DSSScheduleCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    interface Delegate {
        fun onSelectDateIso(view: DSSScheduleCalendarView, isoDate: String)
    }

    var delegate: Delegate? = null

    var selectedDate: Date? = null
        private set

    private var maxDate: Date? = null
    private var minDate: Date? = null
    private var displayedMonth: Int = 0
    private var displayedYear: Int = 0
    private val enabledDays = HashSet<Int>()

    private val calendarUtil: Calendar = Calendar.getInstance()
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val monthFormatter = SimpleDateFormat("MMMM  yyyy", Locale("pt", "BR"))

    private val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val prevButton = TextView(context).apply {
        text = "<"; gravity = Gravity.CENTER
        textSize = 20f; typeface = DSSFont.regular(context, 20f).typeface
        isClickable = true; isFocusable = true
    }
    private val nextButton = TextView(context).apply {
        text = ">"; gravity = Gravity.CENTER
        textSize = 20f; typeface = DSSFont.regular(context, 20f).typeface
        isClickable = true; isFocusable = true
    }
    private val monthLabel = TextView(context).apply {
        textSize = 16f; typeface = DSSFont.bold(context, 16f).typeface
        gravity = Gravity.CENTER
    }
    private val weekdayRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val daysGrid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val descriptionLabel = TextView(context).apply {
        textSize = 18f; typeface = DSSFont.light(context, 18f).typeface
        visibility = View.GONE
    }

    init {
        val pad = 16f.dpToPx(context)
        headerRow.addView(prevButton, LinearLayout.LayoutParams(44f.dpToPx(context), LinearLayout.LayoutParams.WRAP_CONTENT))
        headerRow.addView(monthLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        headerRow.addView(nextButton, LinearLayout.LayoutParams(44f.dpToPx(context), LinearLayout.LayoutParams.WRAP_CONTENT))

        column.addView(
            headerRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = pad; rightMargin = pad },
        )
        column.addView(
            weekdayRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 20f.dpToPx(context); leftMargin = 8f.dpToPx(context); rightMargin = 8f.dpToPx(context) },
        )
        column.addView(
            daysGrid,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 8f.dpToPx(context); leftMargin = 8f.dpToPx(context); rightMargin = 8f.dpToPx(context) },
        )
        column.addView(
            descriptionLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 24f.dpToPx(context); leftMargin = pad; rightMargin = pad },
        )
        addView(column)

        setupWeekdayHeaders()
        prevButton.setOnClickListener { prevMonth() }
        nextButton.setOnClickListener { nextMonth() }

        refresh()
        setupThemeObserver()
    }

    private fun setupWeekdayHeaders() {
        weekdayRow.removeAllViews()
        weekdayRow.weightSum = 7f
        for (d in listOf("D", "S", "T", "Q", "Q", "S", "S")) {
            val l = TextView(context).apply {
                text = d; gravity = Gravity.CENTER
                textSize = 14f
                typeface = DSSFont.bold(context, 14f).typeface
                setTextColor(DSSColors.primary())
            }
            weekdayRow.addView(l, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    /**
     * Configura o calendário com a data máxima e quantos dias para trás são habilitados.
     *
     * @param maxDate Data máxima selecionável (ex: data de renovação).
     * @param daysBack Quantos dias para trás a partir de [maxDate] são habilitados.
     * @param startFromMinDate Se `true`, o calendário abre mostrando o mês da data mínima
     *   em vez da máxima. (iOS: `startFromMinDate`, default `false`.)
     */
    @JvmOverloads
    fun configure(maxDate: Date, daysBack: Int, startFromMinDate: Boolean = false) {
        this.maxDate = maxDate
        val c = Calendar.getInstance().apply { time = maxDate }
        c.add(Calendar.DAY_OF_MONTH, -daysBack)
        this.minDate = c.time
        this.selectedDate = null
        val referenceDate = (if (startFromMinDate) this.minDate else null) ?: maxDate
        calendarUtil.time = referenceDate
        displayedMonth = calendarUtil.get(Calendar.MONTH) + 1
        displayedYear = calendarUtil.get(Calendar.YEAR)
        computeEnabledDays()
        updateMonthLabel()
        updateNavigationButtons()
        buildDaysGrid()
        updateDescriptionLabel()
    }

    fun configure(maxDateIso: String, daysBack: Int) {
        val parsed = try { isoFormatter.parse(maxDateIso) } catch (_: Throwable) { null } ?: return
        configure(parsed, daysBack)
    }

    private fun computeEnabledDays() {
        enabledDays.clear()
        val max = maxDate ?: return
        val min = minDate ?: return
        val maxCal = Calendar.getInstance().apply { time = max }
        val minCal = Calendar.getInstance().apply { time = min }

        val maxDay = maxCal.get(Calendar.DAY_OF_MONTH)
        val maxMonth = maxCal.get(Calendar.MONTH) + 1
        val maxYear = maxCal.get(Calendar.YEAR)
        val minDay = minCal.get(Calendar.DAY_OF_MONTH)
        val minMonth = minCal.get(Calendar.MONTH) + 1
        val minYear = minCal.get(Calendar.YEAR)

        if (displayedMonth == maxMonth && displayedYear == maxYear &&
            displayedMonth == minMonth && displayedYear == minYear
        ) {
            for (d in minDay..maxDay) enabledDays.add(d)
        } else if (displayedMonth == maxMonth && displayedYear == maxYear) {
            for (d in 1..maxDay) enabledDays.add(d)
        } else if (displayedMonth == minMonth && displayedYear == minYear) {
            val cal = Calendar.getInstance().apply { set(minYear, minMonth - 1, 1) }
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (d in minDay..daysInMonth) enabledDays.add(d)
        }
    }

    private fun updateMonthLabel() {
        val cal = Calendar.getInstance().apply { set(displayedYear, displayedMonth - 1, 1) }
        var text = monthFormatter.format(cal.time)
        text = text.replaceFirstChar { it.uppercase() }
        monthLabel.text = text
    }

    private fun updateNavigationButtons() {
        val min = minDate; val max = maxDate
        if (min == null || max == null) {
            prevButton.isEnabled = false; nextButton.isEnabled = false; return
        }
        val minCal = Calendar.getInstance().apply { time = min }
        val maxCal = Calendar.getInstance().apply { time = max }
        val minMonth = minCal.get(Calendar.MONTH) + 1; val minYear = minCal.get(Calendar.YEAR)
        val maxMonth = maxCal.get(Calendar.MONTH) + 1; val maxYear = maxCal.get(Calendar.YEAR)
        prevButton.isEnabled = displayedYear > minYear || (displayedYear == minYear && displayedMonth > minMonth)
        nextButton.isEnabled = displayedYear < maxYear || (displayedYear == maxYear && displayedMonth < maxMonth)
        // iOS: setTitleColor(textPrimary, .normal) / setTitleColor(.systemGray3, .disabled) -> #C7C7CC.
        prevButton.setTextColor(if (prevButton.isEnabled) DSSColors.textPrimary() else android.graphics.Color.parseColor("#C7C7CC"))
        nextButton.setTextColor(if (nextButton.isEnabled) DSSColors.textPrimary() else android.graphics.Color.parseColor("#C7C7CC"))
    }

    private fun prevMonth() {
        if (displayedMonth == 1) { displayedMonth = 12; displayedYear-- } else displayedMonth--
        computeEnabledDays(); updateMonthLabel(); updateNavigationButtons(); buildDaysGrid()
    }
    private fun nextMonth() {
        if (displayedMonth == 12) { displayedMonth = 1; displayedYear++ } else displayedMonth++
        computeEnabledDays(); updateMonthLabel(); updateNavigationButtons(); buildDaysGrid()
    }

    private fun updateDescriptionLabel() {
        val sel = selectedDate
        if (sel == null) {
            descriptionLabel.text = null
            descriptionLabel.visibility = View.GONE
            return
        }
        descriptionLabel.visibility = View.VISIBLE
        val cal = Calendar.getInstance().apply { time = sel }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val dateStr = String.format(Locale.US, "%02d/%02d", day, month)
        val prefix = AppStrings.brand(context, "schedule_calendar_activate_from_prefix", "Ativar programada a partir\ndo dia ")
        val full = prefix + dateStr
        val span = SpannableString(full)
        val start = full.indexOf(dateStr)
        if (start >= 0) {
            val end = start + dateStr.length
            span.setSpan(
                ForegroundColorSpan(DSSColors.primary()),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            // iOS aplica DSSFont.regular(18) na porção da data (base é light(18)).
            span.setSpan(
                TypefaceSpanCompat(DSSFont.regular(context, 18f).typeface),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        descriptionLabel.text = span
        descriptionLabel.setTextColor(DSSColors.textPrimary())
    }

    private fun buildDaysGrid() {
        daysGrid.removeAllViews()
        val cal = Calendar.getInstance().apply { set(displayedYear, displayedMonth - 1, 1) }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
        val offset = firstWeekday - 1
        val totalCells = offset + daysInMonth
        val rows = ceil(totalCells / 7.0).toInt()

        val prevMonthCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevMonthDays = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        var dayNumber = 1
        for (row in 0 until rows) {
            val rowStack = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 7f }
            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val cell: View
                if (cellIndex < offset) {
                    val prevDay = prevMonthDays - (offset - cellIndex - 1)
                    cell = makeGrayDayCell(prevDay)
                } else if (dayNumber > daysInMonth) {
                    val nextDay = dayNumber - daysInMonth
                    cell = makeGrayDayCell(nextDay)
                    dayNumber++
                } else {
                    val day = dayNumber
                    val isEnabled = enabledDays.contains(day)
                    val isSelected = selectedDate?.let {
                        val sc = Calendar.getInstance().apply { time = it }
                        sc.get(Calendar.DAY_OF_MONTH) == day &&
                            sc.get(Calendar.MONTH) + 1 == displayedMonth &&
                            sc.get(Calendar.YEAR) == displayedYear
                    } ?: false
                    cell = makeDayCell(day, isEnabled, isSelected)
                    dayNumber++
                }
                rowStack.addView(cell, LinearLayout.LayoutParams(0, 40f.dpToPx(context), 1f))
            }
            daysGrid.addView(
                rowStack,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = 4f.dpToPx(context) },
            )
        }
    }

    private fun makeGrayDayCell(day: Int): View {
        val container = FrameLayout(context)
        val label = TextView(context).apply {
            text = day.toString(); textSize = 15f
            typeface = DSSFont.regular(context, 15f).typeface
            // iOS: .systemGray3 (literal) para dia fora do mês -> #C7C7CC.
            setTextColor(android.graphics.Color.parseColor("#C7C7CC"))
            gravity = Gravity.CENTER
        }
        container.addView(
            label,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER },
        )
        return container
    }

    private fun makeDayCell(day: Int, enabled: Boolean, selected: Boolean): View {
        val container = FrameLayout(context)
        val label = TextView(context).apply {
            text = day.toString(); textSize = 15f
            gravity = Gravity.CENTER
        }
        when {
            selected -> {
                label.typeface = DSSFont.bold(context, 15f).typeface
                // iOS: setTitleColor(.white) sobre o círculo primary. Aqui o primary pode
                // virar branco no dark/black (algumas brands) -> contrastOnPrimary evita
                // texto branco sumindo no círculo branco.
                label.setTextColor(DSSColors.contrastOnPrimary())
                label.background = DrawableFactory.rounded(
                    context = context,
                    backgroundColor = DSSColors.primary(),
                    cornerRadiusDp = 18f,
                )
                val lp = LayoutParams(36f.dpToPx(context), 36f.dpToPx(context)).apply { gravity = Gravity.CENTER }
                container.addView(label, lp)
            }
            enabled -> {
                label.typeface = DSSFont.bold(context, 15f).typeface
                label.setTextColor(DSSColors.textPrimary())
                container.addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER })
            }
            else -> {
                label.typeface = DSSFont.regular(context, 15f).typeface
                // iOS: .systemGray3 (literal) para dia desabilitado -> #C7C7CC.
                label.setTextColor(android.graphics.Color.parseColor("#C7C7CC"))
                container.addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER })
            }
        }
        if (enabled || selected) {
            container.isClickable = true
            container.setOnClickListener {
                val c = Calendar.getInstance().apply { set(displayedYear, displayedMonth - 1, day) }
                selectedDate = c.time
                buildDaysGrid()
                updateDescriptionLabel()
                delegate?.onSelectDateIso(this, isoFormatter.format(c.time))
            }
        }
        return container
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        monthLabel.setTextColor(DSSColors.textPrimary())
        prevButton.setTextColor(DSSColors.textPrimary())
        nextButton.setTextColor(DSSColors.textPrimary())
        // Reaplica a cor disabled (#C7C7CC) caso a navegação esteja no limite, espelhando
        // o estado .disabled do iOS após troca de tema.
        updateNavigationButtons()
        for (i in 0 until weekdayRow.childCount) {
            (weekdayRow.getChildAt(i) as? TextView)?.setTextColor(DSSColors.primary())
        }
        if (selectedDate != null) {
            buildDaysGrid()
            updateDescriptionLabel()
        }
    }

    /**
     * Span de typeface compatível com minSdk 24 (o construtor `TypefaceSpan(Typeface)`
     * só existe a partir da API 28). Aplica a [typeface] preservando o tamanho do texto base.
     */
    private class TypefaceSpanCompat(private val typeface: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(paint: TextPaint) {
            paint.typeface = typeface
        }

        override fun updateMeasureState(paint: TextPaint) {
            paint.typeface = typeface
        }
    }
}
