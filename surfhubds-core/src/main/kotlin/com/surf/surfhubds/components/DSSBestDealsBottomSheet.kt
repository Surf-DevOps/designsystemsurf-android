package com.surf.surfhubds.components

import android.graphics.Color
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

/**
 * Port do `DSSBestDealsBottomSheet` do iOS — bottom sheet de oferta de upgrade ("Melhor Oferta!").
 *
 * Como o tipo `CatalogSuccess.CustomerResult` vive em SurfAPIKit (iOS), aqui o upgrade
 * vira um `Any?` opaco que o app pode usar no callback.
 */
class DSSBestDealsBottomSheet : BottomSheetDialogFragment() {

    interface Delegate {
        fun bestDealsBottomSheetDidAcceptUpgrade(sheet: DSSBestDealsBottomSheet, upgrade: Any?)
        fun bestDealsBottomSheetDidDecline(sheet: DSSBestDealsBottomSheet)
        fun bestDealsBottomSheetDidDismiss(sheet: DSSBestDealsBottomSheet) {}
    }

    private var offerText: String = ""
    private var upgrade: Any? = null
    var delegate: Delegate? = null

    private var didFireDismiss = false

    fun configure(offerText: String, upgrade: Any?) {
        this.offerText = offerText
        this.upgrade = upgrade
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()

        val scroll = ScrollView(ctx).apply {
            setBackgroundColor(DSSColors.background())
        }

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(ctx), 32f.dpToPx(ctx), 24f.dpToPx(ctx), 24f.dpToPx(ctx))
        }

        val titleLabel = TextView(ctx).apply {
            text = "Melhor Oferta!"
            typeface = DSSFont.bold(ctx, 20f).typeface
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.primary())
        }
        content.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        val descriptionLabel = TextView(ctx).apply {
            text = offerText
            typeface = DSSFont.regular(ctx, 26f).typeface
            textSize = 26f
            gravity = Gravity.CENTER
            // iOS: numberOfLines = 0 (sem limite de linhas)
            maxLines = Int.MAX_VALUE
            setTextColor(DSSColors.textPrimary())
        }
        content.addView(descriptionLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 34f.dpToPx(ctx) })

        val acceptButton = DSSPrincipalButton(ctx).apply {
            text = "Eu quero!"
            // iOS passa backgroundColor: DSSColors.primaryButton (sobrescreve o default primary do botão)
            setBackground(DrawableFactory.rounded(
                context = ctx,
                backgroundColor = DSSColors.primaryButton(),
                cornerRadiusDp = cornerRadiusDp,
            ))
            setTextColor(DSSColors.buttonText())
            onTap = {
                dismiss()
                delegate?.bestDealsBottomSheetDidAcceptUpgrade(this@DSSBestDealsBottomSheet, upgrade)
            }
        }
        content.addView(acceptButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 32f.dpToPx(ctx) })

        val declineButton = AppCompatButton(ctx).apply {
            text = "Não, obrigado(a)"
            isAllCaps = false
            typeface = DSSFont.regular(ctx, 14f).typeface
            textSize = 14f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(DSSColors.primary())
            setOnClickListener {
                dismiss()
                delegate?.bestDealsBottomSheetDidDecline(this@DSSBestDealsBottomSheet)
            }
        }
        content.addView(declineButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 16f.dpToPx(ctx)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        scroll.addView(content, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        return scroll
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (!didFireDismiss) {
            didFireDismiss = true
            delegate?.bestDealsBottomSheetDidDismiss(this)
        }
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            offerText: String,
            upgrade: Any?,
            delegate: Delegate? = null,
        ): DSSBestDealsBottomSheet {
            val sheet = DSSBestDealsBottomSheet()
            sheet.configure(offerText, upgrade)
            sheet.delegate = delegate
            sheet.show(activity.supportFragmentManager, "DSSBestDealsBottomSheet")
            return sheet
        }
    }
}
