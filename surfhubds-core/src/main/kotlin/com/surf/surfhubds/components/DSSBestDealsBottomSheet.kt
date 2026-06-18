package com.surf.surfhubds.components

import android.content.Context
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
import com.surf.surfhubds.brand.Brand
import com.surf.surfhubds.brand.BrandResolver
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.AppStrings
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
            // iOS setupSheet(): .black -> preto; .dark -> rgb(28,28,30); default -> DSSColors.background
            setBackgroundColor(when (ThemeManager.colorScheme) {
                ColorScheme.BLACK -> Color.BLACK
                ColorScheme.DARK -> Color.rgb(28, 28, 30)
                else -> DSSColors.background()
            })
        }

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(ctx), 32f.dpToPx(ctx), 24f.dpToPx(ctx), 24f.dpToPx(ctx))
        }

        val titleLabel = TextView(ctx).apply {
            text = AppStrings.brand(ctx, "best_deals_title", "Melhor Oferta!")
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
            text = AppStrings.brand(ctx, "best_deals_accept", "Eu quero!")
            // iOS: font = DSSFont.regular(16) (botão default é light(16))
            typeface = DSSFont.regular(ctx, 16f).typeface
            // iOS passa backgroundColor: DSSColors.primaryButton + textColor: DSSColors.buttonText.
            // Usa os setters theme-aware p/ não serem sobrescritos pelo refresh() do botão.
            customBackgroundColor = DSSColors.primaryButton()
            customTextColor = DSSColors.buttonText()
            onTap = {
                dismiss()
                delegate?.bestDealsBottomSheetDidAcceptUpgrade(this@DSSBestDealsBottomSheet, upgrade)
            }
        }
        content.addView(acceptButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 32f.dpToPx(ctx) })

        val declineButton = AppCompatButton(ctx).apply {
            text = AppStrings.brand(ctx, "best_deals_decline", "Não, obrigado(a)")
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

    /**
     * Opção de plano usada pelas regras de upsell. Como o tipo
     * `CatalogSuccess.CustomerResult` vive no SurfAPIKit (iOS), aqui o plano vira
     * um par (nome, payload opaco) — o payload volta intacto no
     * [Delegate.bestDealsBottomSheetDidAcceptUpgrade].
     */
    data class PlanOption(
        val noPlano: String,
        val payload: Any? = null,
    )

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

        /**
         * Espelha `presentIfApplicable(from:currentPlan:availablePlans:delegate:)` do iOS:
         * aplica as [OfferUpsellRules] da brand atual e, sem oferta aplicável, chama
         * `didDecline` direto (o fluxo segue como se o usuário tivesse recusado).
         */
        fun presentIfApplicable(
            activity: FragmentActivity,
            currentPlan: PlanOption,
            availablePlans: List<PlanOption>,
            delegate: Delegate,
        ) {
            val offer = OfferUpsellRules.upgrade(activity, currentPlan, availablePlans)
            if (offer == null) {
                delegate.bestDealsBottomSheetDidDecline(DSSBestDealsBottomSheet())
                return
            }
            present(
                activity = activity,
                offerText = offer.text,
                upgrade = offer.upgrade.payload ?: offer.upgrade,
                delegate = delegate,
            )
        }
    }
}

/**
 * Port do enum `OfferUpsellRules` (brand-aware) do `DSSBestDealsBottomSheet.swift` —
 * regras de upsell por brand usadas pelo `presentIfApplicable`.
 */
object OfferUpsellRules {

    data class Offer(
        val upgrade: DSSBestDealsBottomSheet.PlanOption,
        val text: String,
    )

    fun upgrade(
        context: Context,
        current: DSSBestDealsBottomSheet.PlanOption,
        plans: List<DSSBestDealsBottomSheet.PlanOption>,
    ): Offer? = when (BrandResolver.current(context)) {
        Brand.FLACHIP -> flachipUpgrade(context, current, plans)
        Brand.BANDSPORTS -> bandsportsUpgrade(context, current, plans)
        else -> null
    }

    private fun flachipUpgrade(
        context: Context,
        current: DSSBestDealsBottomSheet.PlanOption,
        plans: List<DSSBestDealsBottomSheet.PlanOption>,
    ): Offer? {
        val name = current.noPlano.uppercase()
        if (name.contains("FLA 30")) {
            plans.firstOrNull { it.noPlano.uppercase().contains("FLA 40") }?.let { target ->
                return Offer(
                    upgrade = target,
                    text = AppStrings.brand(context, "best_deals_offer_fla_plus15gb", "Por apenas mais R$10,00 tenha até 15GB A MAIS para usar como quiser."),
                )
            }
        }
        if (name.contains("FLA 40")) {
            plans.firstOrNull { it.noPlano.uppercase().contains("FLA 50") }?.let { target ->
                return Offer(
                    upgrade = target,
                    text = AppStrings.brand(context, "best_deals_offer_fla_plus10gb", "Por apenas mais R$10,00 tenha até 10GB A MAIS para usar como quiser."),
                )
            }
        }
        return null
    }

    private fun bandsportsUpgrade(
        context: Context,
        current: DSSBestDealsBottomSheet.PlanOption,
        plans: List<DSSBestDealsBottomSheet.PlanOption>,
    ): Offer? {
        if (!current.noPlano.contains("Essencial", ignoreCase = true)) return null
        val currentIndex = plans.indexOfFirst { it.noPlano == current.noPlano }
        if (currentIndex < 0 || currentIndex + 1 >= plans.size) return null
        return Offer(
            upgrade = plans[currentIndex + 1],
            text = AppStrings.brand(context, "best_deals_offer_streaming", "Por apenas mais R$10,00 tenha a muito mais canais de conteúdo no streaming Newco Play."),
        )
    }
}
