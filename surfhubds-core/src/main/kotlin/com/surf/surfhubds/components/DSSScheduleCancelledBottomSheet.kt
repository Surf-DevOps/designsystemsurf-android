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
        val scroll = ScrollView(ctx).apply { setBackgroundColor(DSSColors.background()) }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(ctx), 24f.dpToPx(ctx), 24f.dpToPx(ctx), 20f.dpToPx(ctx))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val titleLabel = TextView(ctx).apply {
            text = "Programada cancelada!"
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
            120f.dpToPx(ctx), 100f.dpToPx(ctx),
        ).apply { topMargin = 24f.dpToPx(ctx) })

        val benefits = listOf("GB bônus", "Renovações automáticas", "Mais praticidade")
        val benefitsContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        benefits.forEach { text ->
            val label = TextView(ctx).apply {
                this.text = "✕ $text"
                typeface = DSSFont.regular(ctx, 15f).typeface
                textSize = 15f
                setTextColor(DSSColors.textSecondary())
            }
            benefitsContainer.addView(label, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12f.dpToPx(ctx) })
        }
        root.addView(benefitsContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24f.dpToPx(ctx) })

        val finishButton = DSSPrincipalButton(ctx).apply {
            text = "Finalizar"
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
