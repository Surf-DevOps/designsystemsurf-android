package com.surf.surfhubds.components

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
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSScheduleCancelledBottomSheet` do iOS — sheet de confirmação "Programada cancelada!"
 * com lista de benefícios riscados e botão "Finalizar".
 */
class DSSScheduleCancelledBottomSheet : BottomSheetDialogFragment() {

    interface Delegate {
        fun scheduleCancelledBottomSheetDidFinish(sheet: DSSScheduleCancelledBottomSheet)
    }

    var delegate: Delegate? = null
    var onFinish: (() -> Unit)? = null
    /** Drawable do "cancel_schedule" provido pelo módulo de brand. */
    var illustration: Drawable? = null

    private var didFire = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        // iOS: containerView cornerRadius 24 nos cantos de topo + borderLayer (lineWidth 1)
        // só em black/dark (black -> branco; dark -> branco @40%; light -> sem borda).
        val scroll = ScrollView(ctx).apply { background = roundedTopBorderedBackground(ctx, DSSColors.background()) }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            // iOS: handle top 12, title 24 abaixo do handle, conteudo lateral 24, bottom 20 (safe area).
            setPadding(24f.dpToPx(ctx), 12f.dpToPx(ctx), 24f.dpToPx(ctx), 20f.dpToPx(ctx))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // iOS: handleView 40x5, cornerRadius 2.5, systemGray4, top 12, centralizado.
        val handleView = View(ctx).apply {
            background = com.surf.surfhubds.util.DrawableFactory.rounded(
                context = ctx,
                backgroundColor = 0xFFD1D1D6.toInt(), // systemGray4
                cornerRadiusDp = 2.5f,
            )
        }
        root.addView(handleView, LinearLayout.LayoutParams(
            40f.dpToPx(ctx), 5f.dpToPx(ctx),
        ).apply { gravity = Gravity.CENTER_HORIZONTAL })

        val titleLabel = TextView(ctx).apply {
            text = AppStrings.brand(ctx, "schedule_cancelled_title", "Programada cancelada!")
            typeface = DSSFont.light(ctx, 24f).typeface
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.textPrimary())
        }
        root.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24f.dpToPx(ctx) }) // iOS: titleLabel.top = handleView.bottom + 24

        // iOS carrega `cancel_schedule` via ImageLoader; aqui o param público
        // [illustration] permanece como override opcional.
        val illustrationImageView = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(illustration ?: ImageLoader.image(ctx, "cancel_schedule"))
        }
        root.addView(illustrationImageView, LinearLayout.LayoutParams(
            120f.dpToPx(ctx), 100f.dpToPx(ctx),
        ).apply { topMargin = 24f.dpToPx(ctx) })

        // iOS: stack vertical com linhas [ícone x.circle 18x18 systemGray] + [label systemFont 15 systemGray],
        // spacing 12 entre linhas, alinhamento .leading, bloco centralizado horizontalmente.
        val benefits = listOf(AppStrings.brand(ctx, "schedule_benefit_gb_bonus", "GB bônus"), "Renovações automáticas", "Mais praticidade")
        val benefitsContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
        }
        benefits.forEachIndexed { index, text ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val iconView = ImageView(ctx).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(DSSColors.textSecondary())
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            row.addView(iconView, LinearLayout.LayoutParams(18f.dpToPx(ctx), 18f.dpToPx(ctx)))
            val label = TextView(ctx).apply {
                this.text = text
                typeface = DSSFont.regular(ctx, 15f).typeface
                textSize = 15f
                setTextColor(DSSColors.textSecondary())
            }
            row.addView(label, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 8f.dpToPx(ctx) })
            benefitsContainer.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { if (index > 0) topMargin = 12f.dpToPx(ctx) })
        }
        root.addView(benefitsContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24f.dpToPx(ctx) })

        // Botão primário: bg = primaryButton, texto = buttonText (tokens da brand).
        val finishButton = DSSPrincipalButton(ctx).apply {
            text = AppStrings.brand(ctx, "schedule_cancelled_finish", "Finalizar")
            typeface = DSSFont.regular(ctx, 16f).typeface
            textSize = 16f
            customBackgroundColor = DSSColors.primaryButton()
            customTextColor = DSSColors.buttonText()
            onTap = { finishTapped() }
        }
        root.addView(finishButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 32f.dpToPx(ctx) })

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        return scroll
    }

    private fun finishTapped() {
        if (!didFire) {
            didFire = true
            delegate?.scheduleCancelledBottomSheetDidFinish(this)
            onFinish?.invoke()
        }
        dismiss()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (!didFire) {
            didFire = true
            delegate?.scheduleCancelledBottomSheetDidFinish(this)
            onFinish?.invoke()
        }
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            illustration: Drawable? = null,
            delegate: Delegate? = null,
        ): DSSScheduleCancelledBottomSheet {
            val sheet = DSSScheduleCancelledBottomSheet()
            sheet.delegate = delegate
            sheet.illustration = illustration
            sheet.show(activity.supportFragmentManager, "DSSScheduleCancelledBottomSheet")
            return sheet
        }
    }
}

/**
 * Fundo com cantos superiores arredondados (24dp) + borda do iOS.
 * iOS: borderLayer (lineWidth 1) só em black/dark — black -> branco; dark -> branco @40%.
 */
private fun roundedTopBorderedBackground(ctx: android.content.Context, @androidx.annotation.ColorInt color: Int): Drawable {
    val r = 24f.dpToPx(ctx).toFloat()
    return android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        when (ThemeManager.colorScheme) {
            ColorScheme.BLACK -> setStroke(1f.dpToPx(ctx), android.graphics.Color.WHITE)
            ColorScheme.DARK -> setStroke(1f.dpToPx(ctx), android.graphics.Color.argb(102, 255, 255, 255))
            else -> {}
        }
    }
}
