package com.surf.surfhubds.components

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSScheduleCancelBottomSheet` do iOS — bottom sheet de "Recarga Programada"
 * com lista de benefícios e botões "Manter benefícios" / "Confirmar cancelamento".
 *
 * Ao confirmar o cancelamento dispara [delegate] e [onConfirmCancellation]; quem decide
 * abrir o segundo sheet ("Programada cancelada!") é o app (igual ao iOS).
 */
class DSSScheduleCancelBottomSheet : BottomSheetDialogFragment() {

    interface Delegate {
        /** Chamado quando todo o fluxo de cancelamento terminou. */
        fun scheduleCancelBottomSheetDidFinishCancellation(sheet: DSSScheduleCancelBottomSheet)
    }

    var delegate: Delegate? = null
    /** Closure chamada ao confirmar cancelamento (alternativa ao delegate, permite o app controlar o fluxo). */
    var onConfirmCancellation: (() -> Unit)? = null
    /** Drawable do "keep_schedule" provido pelo módulo de brand. */
    var illustration: Drawable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val isBlack = ThemeManager.colorScheme == ColorScheme.BLACK
        // iOS: containerView cornerRadius 24 nos cantos de topo + borderLayer (lineWidth 1)
        // só em black/dark (black -> branco; dark -> branco @40%; light -> sem borda).
        val scroll = ScrollView(ctx).apply { background = roundedTopBorderedBackground(ctx, DSSColors.background()) }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(ctx), 24f.dpToPx(ctx), 24f.dpToPx(ctx), 20f.dpToPx(ctx))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val titleLabel = TextView(ctx).apply {
            text = AppStrings.brand(ctx, "schedule_cancel_title", "Recarga Programada")
            typeface = DSSFont.light(ctx, 24f).typeface
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.textPrimary())
        }
        root.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        val illustrationImageView = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(illustration)
        }
        root.addView(illustrationImageView, LinearLayout.LayoutParams(
            120f.dpToPx(ctx), 80f.dpToPx(ctx),
        ).apply { topMargin = 24f.dpToPx(ctx) })

        val benefits = listOf(
            AppStrings.brand(ctx, "schedule_benefit_gb_bonus", "GB bônus"),
            AppStrings.brand(ctx, "schedule_benefit_auto_renewals", "Renovações automáticas"),
            AppStrings.brand(ctx, "schedule_benefit_more_convenience", "Mais praticidade"),
        )
        val benefitsContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        // iOS: colorScheme == .black ? .systemRed : DSSColors.primary
        // .systemRed -> #FF3B30 (cor literal hardcoded também no iOS)
        val benefitColor = if (isBlack) Color.parseColor("#FF3B30") else DSSColors.primary()
        benefits.forEach { text ->
            val label = TextView(ctx).apply {
                this.text = "✓ $text"
                // iOS: .systemFont(ofSize: 15) — fonte de sistema, não DSSFont
                textSize = 15f
                maxLines = Int.MAX_VALUE
                setTextColor(benefitColor)
            }
            benefitsContainer.addView(label, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12f.dpToPx(ctx) })
        }
        root.addView(benefitsContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24f.dpToPx(ctx) })

        // Botão primário: bg = primaryButton, texto = buttonText (tokens da brand); font regular(16).
        val keepButton = DSSPrincipalButton(ctx).apply {
            text = AppStrings.brand(ctx, "schedule_cancel_keep_benefits", "Manter benefícios")
            customBackgroundColor = DSSColors.primaryButton()
            customTextColor = DSSColors.buttonText()
            typeface = DSSFont.regular(ctx, 16f).typeface
            textSize = 16f
            onTap = { dismiss() }
        }
        root.addView(keepButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 32f.dpToPx(ctx) })

        val cancelButton = AppCompatButton(ctx).apply {
            text = AppStrings.brand(ctx, "schedule_cancel_confirm_cancel", "Confirmar cancelamento")
            isAllCaps = false
            typeface = DSSFont.regular(ctx, 16f).typeface
            textSize = 16f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(DSSColors.primary())
            setOnClickListener { confirmCancelTapped() }
        }
        root.addView(cancelButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 16f.dpToPx(ctx)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        return scroll
    }

    private fun confirmCancelTapped() {
        // iOS: captura delegate + onConfirmCancellation, anima a saída e dispara ambos
        // (delegate primeiro, depois a closure). O iOS NÃO abre o segundo sheet
        // automaticamente — isso fica a cargo do app via delegate/closure.
        val capturedDelegate = delegate
        val capturedOnConfirm = onConfirmCancellation
        dismiss()
        capturedDelegate?.scheduleCancelBottomSheetDidFinishCancellation(this)
        capturedOnConfirm?.invoke()
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            illustration: Drawable? = null,
            delegate: Delegate? = null,
            onConfirmCancellation: (() -> Unit)? = null,
        ): DSSScheduleCancelBottomSheet {
            val sheet = DSSScheduleCancelBottomSheet()
            sheet.delegate = delegate
            sheet.onConfirmCancellation = onConfirmCancellation
            sheet.illustration = illustration
            sheet.show(activity.supportFragmentManager, "DSSScheduleCancelBottomSheet")
            return sheet
        }
    }
}

/**
 * Fundo com cantos superiores arredondados (24dp) + borda do iOS.
 * iOS: borderLayer (lineWidth 1) só em black/dark — black -> branco; dark -> branco @40%.
 */
private fun roundedTopBorderedBackground(ctx: android.content.Context, @androidx.annotation.ColorInt color: Int): android.graphics.drawable.Drawable {
    val r = 24f.dpToPx(ctx).toFloat()
    return android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        when (ThemeManager.colorScheme) {
            ColorScheme.BLACK -> setStroke(1f.dpToPx(ctx), Color.WHITE)
            ColorScheme.DARK -> setStroke(1f.dpToPx(ctx), Color.argb(102, 255, 255, 255))
            else -> {}
        }
    }
}
