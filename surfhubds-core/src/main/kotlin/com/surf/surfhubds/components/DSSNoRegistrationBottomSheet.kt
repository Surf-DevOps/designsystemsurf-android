package com.surf.surfhubds.components

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.R
import com.surf.surfhubds.brand.BrandInfo
import com.surf.surfhubds.brand.BrandResolver
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSNoRegistrationBottomSheet` do iOS — bottom sheet exibido quando o login
 * responde sucesso mas o documento informado não possui cadastro na base. Traz ícone
 * circular, título com o nome da brand, mensagem e o botão "Cadastrar".
 *
 * O "chrome" do iOS (cantos superiores 24, blur de fundo, toque fora → dismiss e
 * swipe-to-dismiss) já é aplicado automaticamente pelo
 * [com.surf.surfhubds.util.DSSBottomSheetChrome] + `BottomSheetDialog`.
 */
class DSSNoRegistrationBottomSheet : BottomSheetDialogFragment() {

    interface Delegate {
        /** Chamado quando o usuário toca em "Cadastrar". */
        fun noRegistrationBottomSheetDidTapRegister(sheet: DSSNoRegistrationBottomSheet) {}

        /** Chamado quando o sheet é fechado sem cadastrar (toque fora ou swipe). */
        fun noRegistrationBottomSheetDidDismiss(sheet: DSSNoRegistrationBottomSheet) {}
    }

    var delegate: Delegate? = null

    private var didTapRegister = false
    private var didFireDismiss = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val scheme = ThemeManager.colorScheme

        // iOS containerView: fundo por scheme (.black → preto; .dark → rgb(28,28,30);
        // default → .systemBackground), cantos superiores 24 e borda 1pt adicionada só
        // nos schemes black/dark (.black → branco; .dark → branco 40%); no default o iOS
        // NÃO adiciona o borderLayer (usa sombra).
        val containerColor = when (scheme) {
            ColorScheme.BLACK -> Color.BLACK
            ColorScheme.DARK -> Color.rgb(28, 28, 30)
            else -> DSSColors.background()
        }
        val borderColor = when (scheme) {
            ColorScheme.BLACK -> Color.WHITE
            ColorScheme.DARK -> Color.argb(0x66, 0xFF, 0xFF, 0xFF) // branco 40%
            else -> null
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
            // iOS: handle top = container.top + 12; insets horizontais = 24;
            // registerButton bottom = safeArea.bottom - 24.
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
            background = DrawableFactory.rounded(
                context = ctx, backgroundColor = handleColor, cornerRadiusDp = 2.5f,
            )
        }
        root.addView(handle, LinearLayout.LayoutParams(40f.dpToPx(ctx), 5f.dpToPx(ctx)))

        // Ícone: círculo 80x80 com DSSColors.success e o glyph branco 32pt centralizado
        // (iOS: SF Symbol "person.badge.plus", pointSize 32, weight .medium, tint branco).
        val iconContainer = FrameLayout(ctx).apply {
            background = DrawableFactory.rounded(
                context = ctx, backgroundColor = DSSColors.success(), cornerRadiusDp = 40f,
            )
        }
        val icon = ImageView(ctx).apply {
            setImageResource(R.drawable.dss_person_badge_plus)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setColorFilter(Color.WHITE)
        }
        iconContainer.addView(icon, FrameLayout.LayoutParams(
            32f.dpToPx(ctx), 32f.dpToPx(ctx), Gravity.CENTER,
        ))
        root.addView(iconContainer, LinearLayout.LayoutParams(
            80f.dpToPx(ctx), 80f.dpToPx(ctx),
        ).apply { topMargin = 24f.dpToPx(ctx) })

        // Título — iOS: DSSFont.bold(20), textPrimary, centralizado, formatado com o
        // mvnoName da brand corrente.
        val titleLabel = TextView(ctx).apply {
            text = title(ctx)
            typeface = DSSFont.bold(ctx, 20f).typeface
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.textPrimary())
        }
        root.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 20f.dpToPx(ctx) })

        // Mensagem — iOS: DSSFont.regular(15), textSecondary, centralizada.
        val messageLabel = TextView(ctx).apply {
            text = AppStrings.brand(ctx, "no_registration_message", MESSAGE)
            typeface = DSSFont.regular(ctx, 15f).typeface
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(DSSColors.textSecondary())
        }
        root.addView(messageLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 12f.dpToPx(ctx) })

        // Botão "Cadastrar" — iOS usa o DSSPrincipalButton com os defaults do init
        // (background = primary/primaryButton, textColor = buttonText), altura 50.
        val registerButton = DSSPrincipalButton(ctx).apply {
            text = AppStrings.brand(ctx, "no_registration_register_button", REGISTER_BUTTON)
            onTap = { registerTapped() }
        }
        root.addView(registerButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 28f.dpToPx(ctx) })

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        return scroll
    }

    private fun title(ctx: Context): String {
        val brandName = BrandInfo.current(BrandResolver.current(ctx)).mvnoName
        return AppStrings.brand(ctx, "no_registration_title_format", TITLE_FORMAT, brandName)
    }

    private fun registerTapped() {
        didTapRegister = true
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (didFireDismiss) return
        didFireDismiss = true
        // iOS: o didTapRegister é disparado após a animação de fechamento; o didDismiss
        // só nos fechamentos por toque fora / swipe.
        if (didTapRegister) {
            delegate?.noRegistrationBottomSheetDidTapRegister(this)
        } else {
            delegate?.noRegistrationBottomSheetDidDismiss(this)
        }
    }

    companion object {
        private const val TITLE_FORMAT = "Você ainda não tem cadastro na %s"
        private const val MESSAGE =
            "Não encontramos esse documento na nossa base. Faça seu cadastro para continuar."
        private const val REGISTER_BUTTON = "Cadastrar"

        fun present(
            activity: FragmentActivity,
            delegate: Delegate? = null,
        ): DSSNoRegistrationBottomSheet {
            val sheet = DSSNoRegistrationBottomSheet()
            sheet.delegate = delegate
            sheet.show(activity.supportFragmentManager, "DSSNoRegistrationBottomSheet")
            return sheet
        }
    }
}
