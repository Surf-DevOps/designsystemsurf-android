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
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSScheduleCancelBottomSheet` do iOS — bottom sheet de "Recarga Programada"
 * com lista de benefícios e botões "Manter benefícios" / "Confirmar cancelamento".
 *
 * Após confirmar o cancelamento abre automaticamente o [DSSScheduleCancelledBottomSheet].
 */
class DSSScheduleCancelBottomSheet : BottomSheetDialogFragment() {

    interface Delegate {
        /** Chamado quando todo o fluxo de cancelamento terminou. */
        fun scheduleCancelBottomSheetDidFinishCancellation(sheet: DSSScheduleCancelBottomSheet)
    }

    var delegate: Delegate? = null
    /** Drawable do "keep_schedule" provido pelo módulo de brand. */
    var illustration: Drawable? = null

    private var hostActivity: FragmentActivity? = null

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
            text = "Recarga Programada"
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

        val benefits = listOf("GB bônus", "Renovações automáticas", "Mais praticidade")
        val benefitsContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        benefits.forEach { text ->
            val label = TextView(ctx).apply {
                this.text = "✓ $text"
                typeface = DSSFont.regular(ctx, 15f).typeface
                textSize = 15f
                setTextColor(DSSColors.primary())
            }
            benefitsContainer.addView(label, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12f.dpToPx(ctx) })
        }
        root.addView(benefitsContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24f.dpToPx(ctx) })

        val keepButton = DSSPrincipalButton(ctx).apply {
            text = "Manter benefícios"
            onTap = { dismiss() }
        }
        root.addView(keepButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 32f.dpToPx(ctx) })

        val cancelButton = AppCompatButton(ctx).apply {
            text = "Confirmar cancelamento"
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
        val capturedDelegate = delegate
        val capturedHost = hostActivity
        dismiss()
        capturedDelegate?.scheduleCancelBottomSheetDidFinishCancellation(this)
        capturedHost?.let {
            DSSScheduleCancelledBottomSheet.present(it)
        }
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            illustration: Drawable? = null,
            delegate: Delegate? = null,
        ): DSSScheduleCancelBottomSheet {
            val sheet = DSSScheduleCancelBottomSheet()
            sheet.delegate = delegate
            sheet.illustration = illustration
            sheet.hostActivity = activity
            sheet.show(activity.supportFragmentManager, "DSSScheduleCancelBottomSheet")
            return sheet
        }
    }
}
