package com.surf.surfhubds.components

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
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
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSBenefitRedeemedBottomSheet` do iOS — bottom sheet de sucesso após resgatar
 * um benefício, com ícone/ilustração, título, mensagem de bônus, detalhe opcional e botões
 * "Resgatar novo benefício" / "Concluir".
 */
class DSSBenefitRedeemedBottomSheet : BottomSheetDialogFragment() {

    interface Delegate {
        /** Chamado quando o usuário toca em "Resgatar novo benefício". */
        fun benefitRedeemedBottomSheetDidTapNewRedeem(sheet: DSSBenefitRedeemedBottomSheet) {}

        /** Chamado quando o bottom sheet é fechado (concluir, toque fora ou swipe). */
        fun benefitRedeemedBottomSheetDidDismiss(sheet: DSSBenefitRedeemedBottomSheet) {}
    }

    var delegate: Delegate? = null

    /** Ilustração "ilBenefitSuccess" provida pelo módulo de brand. */
    var illustration: Drawable? = null

    private var bonusText: String = ""
    private var detailText: String? = null

    private var didTapNewRedeem = false
    private var didFireDismiss = false

    private var detailLabel: TextView? = null
    private var messageLabel: TextView? = null

    fun configure(bonusText: String, detailText: String?) {
        this.bonusText = bonusText
        this.detailText = detailText
        messageLabel?.text = String.format(SUCCESS_MESSAGE_FORMAT, bonusText)
        detailLabel?.let {
            it.text = detailText
            it.visibility = if (detailText == null) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val scheme = ThemeManager.colorScheme
        // iOS containerView: fundo por scheme (.black → preto; .dark → rgb(28,28,30);
        // default → .systemBackground), cantos superiores 24 e borda 1pt (lineWidth=1)
        // adicionada só nos schemes black/dark (.black → branco; .dark → branco 40%);
        // no default o iOS NÃO adiciona o borderLayer.
        val containerColor = when (scheme) {
            ColorScheme.BLACK -> Color.BLACK
            ColorScheme.DARK -> Color.rgb(28, 28, 30)
            else -> DSSColors.background()
        }
        val borderColor = when (scheme) {
            ColorScheme.BLACK -> Color.WHITE
            ColorScheme.DARK -> Color.argb(0x66, 0xFF, 0xFF, 0xFF) // branco 40%
            else -> null // default não tem borda no iOS
        }
        val scroll = ScrollView(ctx).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(containerColor)
                val r = 24f.dpToPx(ctx).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                if (borderColor != null) setStroke(1f.dpToPx(ctx), borderColor)
            }
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            // iOS: handle top = container.top + 12; horizontal insets = 24; finishButton bottom = safeArea.bottom - 24
            setPadding(24f.dpToPx(ctx), 12f.dpToPx(ctx), 24f.dpToPx(ctx), 24f.dpToPx(ctx))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Handle — iOS: .black → branco 40%; .dark → branco 30%; default → systemGray4.
        val handleColor = when (scheme) {
            ColorScheme.BLACK -> Color.argb(0x66, 0xFF, 0xFF, 0xFF) // branco 40%
            ColorScheme.DARK -> Color.argb(0x4D, 0xFF, 0xFF, 0xFF) // branco 30%
            else -> Color.rgb(209, 209, 214) // systemGray4 (light)
        }
        val handle = View(ctx).apply {
            background = com.surf.surfhubds.util.DrawableFactory.rounded(
                context = ctx, backgroundColor = handleColor, cornerRadiusDp = 2.5f,
            )
        }
        root.addView(handle, LinearLayout.LayoutParams(40f.dpToPx(ctx), 5f.dpToPx(ctx)))

        // Success icon / brand illustration.
        // iOS: ImageLoader.image(named: "ilBenefitSuccess") ?? SF Symbol "checkmark.seal.fill",
        // com tintColor = DSSColors.primary.
        val successIcon = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            val explicit = illustration
            if (explicit != null) {
                setImageDrawable(explicit)
            } else {
                val brandImage = ImageLoader.image(ctx, "ilbenefitsuccess")
                if (brandImage != null) {
                    setImageDrawable(brandImage)
                } else {
                    setImageResource(android.R.drawable.checkbox_on_background)
                }
            }
            // iOS: iv.tintColor = DSSColors.primary aplicado em todos os casos.
            setColorFilter(DSSColors.primary())
        }
        root.addView(successIcon, LinearLayout.LayoutParams(
            80f.dpToPx(ctx), 80f.dpToPx(ctx),
        ).apply { topMargin = 24f.dpToPx(ctx) })

        // Title
        val titleLabel = TextView(ctx).apply {
            text = SUCCESS_TITLE
            typeface = DSSFont.bold(ctx, 20f).typeface
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.primary())
        }
        root.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 20f.dpToPx(ctx) })

        // Message
        val messageLabel = TextView(ctx).apply {
            text = String.format(SUCCESS_MESSAGE_FORMAT, bonusText)
            typeface = DSSFont.regular(ctx, 16f).typeface
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.textPrimary())
        }
        this.messageLabel = messageLabel
        root.addView(messageLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 12f.dpToPx(ctx) })

        // Detail (optional)
        val detailLabel = TextView(ctx).apply {
            text = detailText
            typeface = DSSFont.regular(ctx, 13f).typeface
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.textSecondary())
            visibility = if (detailText == null) View.GONE else View.VISIBLE
        }
        this.detailLabel = detailLabel
        root.addView(detailLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 8f.dpToPx(ctx) })

        // "Resgatar novo benefício" button
        // iOS: backgroundColor = DSSColors.primaryButton, textColor = DSSColors.buttonText,
        // font = DSSFont.regular(16) (DSSPrincipalButton default é light(16)).
        val newRedeemButton = DSSPrincipalButton(ctx).apply {
            text = NEW_REDEEM
            typeface = DSSFont.regular(ctx, 16f).typeface
            customBackgroundColor = DSSColors.primaryButton()
            customTextColor = DSSColors.buttonText()
            onTap = { newRedeemTapped() }
        }
        root.addView(newRedeemButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 28f.dpToPx(ctx) })

        // "Concluir" button
        val finishButton = AppCompatButton(ctx).apply {
            text = FINISH
            isAllCaps = false
            typeface = DSSFont.regular(ctx, 16f).typeface
            textSize = 16f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(DSSColors.textPrimary())
            setOnClickListener { dismiss() }
        }
        root.addView(finishButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 12f.dpToPx(ctx)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        return scroll
    }

    private fun newRedeemTapped() {
        didTapNewRedeem = true
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (didFireDismiss) return
        didFireDismiss = true
        if (didTapNewRedeem) {
            delegate?.benefitRedeemedBottomSheetDidTapNewRedeem(this)
        } else {
            delegate?.benefitRedeemedBottomSheetDidDismiss(this)
        }
    }

    companion object {
        private const val SUCCESS_TITLE = "Benefício resgatado!"
        private const val SUCCESS_MESSAGE_FORMAT = "Você resgatou %s de bônus."
        private const val NEW_REDEEM = "Resgatar novo benefício"
        private const val FINISH = "Concluir"

        fun present(
            activity: FragmentActivity,
            bonusText: String,
            detailText: String? = null,
            illustration: Drawable? = null,
            delegate: Delegate? = null,
        ): DSSBenefitRedeemedBottomSheet {
            val sheet = DSSBenefitRedeemedBottomSheet()
            sheet.delegate = delegate
            sheet.illustration = illustration
            sheet.configure(bonusText, detailText)
            sheet.show(activity.supportFragmentManager, "DSSBenefitRedeemedBottomSheet")
            return sheet
        }
    }
}
