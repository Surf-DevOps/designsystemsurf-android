package com.surf.surfhubds.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.dpToPx
import java.util.Date

/**
 * Port do `DSSScheduleCalendarBottomSheet` do iOS — bottom sheet de calendário para
 * agendamento de recarga. Usa o [DSSScheduleCalendarView] que respeita data máxima e
 * janela retroativa (qtDiaValidade).
 */
class DSSScheduleCalendarBottomSheet : BottomSheetDialogFragment(), DSSScheduleCalendarView.Delegate {

    interface Delegate {
        fun scheduleCalendarBottomSheetDidSelectDateISO(sheet: DSSScheduleCalendarBottomSheet, dateISO: String)
    }

    var delegate: Delegate? = null

    private var maxDateISO: String? = null
    private var maxDate: Date? = null
    private var daysBack: Int = 0
    private var selectedDateISO: String? = null

    private var calendarView: DSSScheduleCalendarView? = null

    fun configure(maxDateISO: String, daysBack: Int) {
        this.maxDateISO = maxDateISO
        this.maxDate = null
        this.daysBack = daysBack
        calendarView?.configure(maxDateIso = maxDateISO, daysBack = daysBack)
    }

    fun configure(maxDate: Date, daysBack: Int) {
        this.maxDate = maxDate
        this.maxDateISO = null
        this.daysBack = daysBack
        calendarView?.configure(maxDate = maxDate, daysBack = daysBack)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(DSSColors.background())
            setPadding(16f.dpToPx(ctx), 20f.dpToPx(ctx), 16f.dpToPx(ctx), 20f.dpToPx(ctx))
        }

        val calendar = DSSScheduleCalendarView(ctx).apply {
            delegate = this@DSSScheduleCalendarBottomSheet
        }
        calendarView = calendar
        root.addView(calendar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        // Aplicar config pendente
        maxDateISO?.let { calendar.configure(maxDateIso = it, daysBack = daysBack) }
        maxDate?.let { calendar.configure(maxDate = it, daysBack = daysBack) }

        val confirmButton = DSSPrincipalButton(ctx).apply {
            text = "Confirmar"
            onTap = { confirmTapped() }
        }
        root.addView(confirmButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 24f.dpToPx(ctx) })

        return root
    }

    override fun onSelectDateIso(view: DSSScheduleCalendarView, isoDate: String) {
        selectedDateISO = isoDate
    }

    private fun confirmTapped() {
        val iso = selectedDateISO ?: return
        delegate?.scheduleCalendarBottomSheetDidSelectDateISO(this, iso)
        dismiss()
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            maxDateISO: String,
            daysBack: Int,
            delegate: Delegate? = null,
        ): DSSScheduleCalendarBottomSheet {
            val sheet = DSSScheduleCalendarBottomSheet()
            sheet.delegate = delegate
            sheet.configure(maxDateISO = maxDateISO, daysBack = daysBack)
            sheet.show(activity.supportFragmentManager, "DSSScheduleCalendarBottomSheet")
            return sheet
        }

        fun present(
            activity: FragmentActivity,
            maxDate: Date,
            daysBack: Int,
            delegate: Delegate? = null,
        ): DSSScheduleCalendarBottomSheet {
            val sheet = DSSScheduleCalendarBottomSheet()
            sheet.delegate = delegate
            sheet.configure(maxDate = maxDate, daysBack = daysBack)
            sheet.show(activity.supportFragmentManager, "DSSScheduleCalendarBottomSheet")
            return sheet
        }
    }
}
