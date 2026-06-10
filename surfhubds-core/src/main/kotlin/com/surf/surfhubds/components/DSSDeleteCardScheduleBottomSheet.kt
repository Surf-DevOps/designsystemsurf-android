package com.surf.surfhubds.components

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/** Opção selecionada no [DSSDeleteCardScheduleBottomSheet]. Espelha o enum iOS. */
enum class DSSDeleteCardScheduleOption {
    TRANSFER_TO_ANOTHER_CARD,
    CANCEL_SCHEDULE,
}

/**
 * Port do `DSSDeleteCardScheduleBottomSheet` do iOS — bottom sheet com duas opções
 * selecionáveis ("Transferir para outro cartão" / "Cancelar programação") e um botão
 * de confirmação que reporta a opção escolhida via [onConfirm].
 */
class DSSDeleteCardScheduleBottomSheet : BottomSheetDialogFragment() {

    interface Delegate {
        fun deleteCardScheduleBottomSheetDidConfirm(
            sheet: DSSDeleteCardScheduleBottomSheet,
            option: DSSDeleteCardScheduleOption,
        )
    }

    var delegate: Delegate? = null
    var onConfirm: ((DSSDeleteCardScheduleOption) -> Unit)? = null
    /** Ilustração opcional provida pelo módulo de brand. */
    var illustration: Drawable? = null

    private var selectedOption = DSSDeleteCardScheduleOption.TRANSFER_TO_ANOTHER_CARD

    private lateinit var transferButton: TextView
    private lateinit var cancelScheduleButton: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply { setBackgroundColor(DSSColors.background()) }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(ctx), 24f.dpToPx(ctx), 24f.dpToPx(ctx), 24f.dpToPx(ctx))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val titleLabel = TextView(ctx).apply {
            text = "Excluir cartão"
            typeface = DSSFont.regular(ctx, 18f).typeface
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.textPrimary())
        }
        root.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            // iOS: title leading/trailing = 32pt; root já tem 24, +8 para chegar a 32.
            leftMargin = 8f.dpToPx(ctx)
            rightMargin = 8f.dpToPx(ctx)
        })

        val subtitleLabel = TextView(ctx).apply {
            text = "Este cartão está vinculado à sua recarga programada. O que deseja fazer?"
            typeface = DSSFont.bold(ctx, 18f).typeface
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.primary())
        }
        root.addView(subtitleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 12f.dpToPx(ctx)
            // iOS: subtitle leading/trailing = 32pt; root já tem 24, +8 para chegar a 32.
            leftMargin = 8f.dpToPx(ctx)
            rightMargin = 8f.dpToPx(ctx)
        })

        transferButton = makeOptionButton(ctx, "Transferir programada para outro cartão") {
            selectedOption = DSSDeleteCardScheduleOption.TRANSFER_TO_ANOTHER_CARD
            updateSelection()
        }
        root.addView(transferButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24f.dpToPx(ctx) })

        cancelScheduleButton = makeOptionButton(ctx, "Entrar em contato e solicitar o cancelamento da programada.") {
            selectedOption = DSSDeleteCardScheduleOption.CANCEL_SCHEDULE
            updateSelection()
        }
        root.addView(cancelScheduleButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 12f.dpToPx(ctx) })

        // iOS: confirmButton é um UIButton "cru" com fill = DSSColors.primaryButton
        // e texto = DSSColors.buttonText (NÃO usa DSSPrincipalButton, que pinta com `primary`).
        val confirmButton = AppCompatButton(ctx).apply {
            text = "Confirmar"
            isAllCaps = false
            gravity = Gravity.CENTER
            textSize = 18f
            typeface = DSSFont.bold(ctx, 18f).typeface
            setTextColor(DSSColors.buttonText())
            background = DrawableFactory.rounded(
                ctx,
                backgroundColor = DSSColors.primaryButton(),
                cornerRadiusDp = 28f,
            )
            setOnClickListener { confirmTapped() }
        }
        root.addView(confirmButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 56f.dpToPx(ctx),
        ).apply { topMargin = 24f.dpToPx(ctx) })

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))

        updateSelection()
        return scroll
    }

    private fun makeOptionButton(
        ctx: android.content.Context,
        label: String,
        onClick: () -> Unit,
    ): TextView = TextView(ctx).apply {
        text = label
        typeface = DSSFont.medium(ctx, 16f).typeface
        textSize = 16f
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        setTextColor(DSSColors.textPrimary())
        setPadding(20f.dpToPx(ctx), 16f.dpToPx(ctx), 20f.dpToPx(ctx), 16f.dpToPx(ctx))
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

    private fun updateSelection() {
        val ctx = context ?: return
        applyOptionStyle(ctx, transferButton, selectedOption == DSSDeleteCardScheduleOption.TRANSFER_TO_ANOTHER_CARD)
        applyOptionStyle(ctx, cancelScheduleButton, selectedOption == DSSDeleteCardScheduleOption.CANCEL_SCHEDULE)
    }

    private fun applyOptionStyle(ctx: android.content.Context, view: TextView, selected: Boolean) {
        val strokeColor = if (selected) DSSColors.primary() else DSSColors.borderDefault()
        val strokeWidth = if (selected) 2f else 1f
        view.background = DrawableFactory.rounded(
            ctx,
            backgroundColor = Color.TRANSPARENT,
            cornerRadiusDp = 12f,
            strokeColor = strokeColor,
            strokeWidthDp = strokeWidth,
        )
    }

    private fun confirmTapped() {
        val option = selectedOption
        dismiss()
        delegate?.deleteCardScheduleBottomSheetDidConfirm(this, option)
        onConfirm?.invoke(option)
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            illustration: Drawable? = null,
            delegate: Delegate? = null,
            onConfirm: ((DSSDeleteCardScheduleOption) -> Unit)? = null,
        ): DSSDeleteCardScheduleBottomSheet {
            val sheet = DSSDeleteCardScheduleBottomSheet()
            sheet.delegate = delegate
            sheet.illustration = illustration
            sheet.onConfirm = onConfirm
            sheet.show(activity.supportFragmentManager, "DSSDeleteCardScheduleBottomSheet")
            return sheet
        }
    }
}
