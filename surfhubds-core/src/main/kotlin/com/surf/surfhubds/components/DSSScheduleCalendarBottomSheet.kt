package com.surf.surfhubds.components

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dpToPxFloat
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
        val scheme = ThemeManager.colorScheme

        // Container com cantos superiores arredondados (iOS: cornerRadius 24 nos cantos de topo).
        val containerBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(DSSColors.background())
            val r = 24f.dpToPxFloat(ctx)
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            // iOS: borderLayer (lineWidth 1) adicionado só em black/dark.
            // black -> branco; dark -> branco @40%.
            when (scheme) {
                ColorScheme.BLACK -> setStroke(1f.dpToPx(ctx), android.graphics.Color.WHITE)
                ColorScheme.DARK -> setStroke(
                    1f.dpToPx(ctx),
                    android.graphics.Color.argb(102, 255, 255, 255),
                )
                else -> {}
            }
        }
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = containerBg
            // bottom = safeArea -20 no iOS; topo cuidado pelo handle (margin 12).
            setPadding(0, 0, 0, 20f.dpToPx(ctx))
        }

        // Handle (iOS: 40x5, cornerRadius 2.5, topMargin 12, centralizado).
        val handle = View(ctx).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(DSSColors.divider())
                cornerRadius = 2.5f.dpToPxFloat(ctx)
            }
        }
        root.addView(
            handle,
            LinearLayout.LayoutParams(40f.dpToPx(ctx), 5f.dpToPx(ctx)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = 12f.dpToPx(ctx)
            },
        )

        val calendar = DSSScheduleCalendarView(ctx).apply {
            delegate = this@DSSScheduleCalendarBottomSheet
        }
        calendarView = calendar
        // iOS: calendar leading/trailing 16, top = handle.bottom + 20.
        root.addView(calendar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 20f.dpToPx(ctx)
            leftMargin = 16f.dpToPx(ctx)
            rightMargin = 16f.dpToPx(ctx)
        })

        // Aplicar config pendente
        maxDateISO?.let { calendar.configure(maxDateIso = it, daysBack = daysBack) }
        maxDate?.let { calendar.configure(maxDate = it, daysBack = daysBack) }

        // Botão primário: bg = primaryButton, texto = buttonText (tokens da brand); font regular(16).
        val confirmButton = DSSPrincipalButton(ctx).apply {
            text = AppStrings.brand(ctx, "schedule_calendar_confirm", "Confirmar")
            customBackgroundColor = DSSColors.primaryButton()
            customTextColor = DSSColors.buttonText()
            typeface = DSSFont.regular(ctx, 16f).typeface
            onTap = { confirmTapped() }
        }
        // iOS: confirm leading/trailing 24, top = calendar.bottom + 24, height 50.
        root.addView(confirmButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply {
            topMargin = 24f.dpToPx(ctx)
            leftMargin = 24f.dpToPx(ctx)
            rightMargin = 24f.dpToPx(ctx)
        })

        return root
    }

    override fun onSelectDateIso(view: DSSScheduleCalendarView, isoDate: String) {
        selectedDateISO = isoDate
    }

    private fun confirmTapped() {
        val iso = selectedDateISO ?: return
        // iOS: animateDismissal { delegate.didSelectDateISO } — dispara o delegate
        // só após o sheet ser dispensado (delegate capturado antes do dismiss).
        val capturedDelegate = delegate
        dismiss()
        capturedDelegate?.scheduleCalendarBottomSheetDidSelectDateISO(this, iso)
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
