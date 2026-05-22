package com.surf.surfhubds.components

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSRechargeBottomSheet` do iOS — bottom sheet de confirmação de recarga
 * com seleção de método de pagamento (cartão/PIX), agendamento e bônus.
 *
 * No iOS este componente depende de `Card`, `DSSResumeCard` e `DSSPaymentSelectionView`
 * que ainda não foram portados. Aqui exponho a mesma superfície pública (delegate,
 * `configure`, etc) usando tipos opacos `Any?` para os cartões — as bordas se conectam
 * ao app via callback.
 */
class DSSRechargeBottomSheet : BottomSheetDialogFragment() {

    // MARK: - Delegate

    interface Delegate {
        fun rechargeBottomSheetDidConfirmWithCard(sheet: DSSRechargeBottomSheet, card: Any?, scheduled: Boolean)
        fun rechargeBottomSheetDidConfirmWithPix(sheet: DSSRechargeBottomSheet, scheduled: Boolean)
        fun rechargeBottomSheetDidTapAddCard(sheet: DSSRechargeBottomSheet)
        fun rechargeBottomSheetDidDismiss(sheet: DSSRechargeBottomSheet) {}
        fun rechargeBottomSheetDidChangeScheduleStatus(sheet: DSSRechargeBottomSheet, isScheduled: Boolean) {}
    }

    enum class PaymentMethod { PIX, CREDIT_CARD, NEW_CARD }

    // MARK: - Configuration

    data class Configuration(
        val title: String = "Confirme sua recarga",
        val phoneNumber: String,
        val offerName: String,
        val priceInCents: Int,
        val showScheduleOption: Boolean = true,
        val scheduleBonus: String? = null,
    )

    var delegate: Delegate? = null

    private var configuration: Configuration? = null
    private var creditCards: List<Any> = emptyList()
    private var showPix = false
    private var showOnlyPix = false
    private var showOnlyCards = false
    private var hideSchedule = false
    private var isScheduled = true
    private var selectedMethod: PaymentMethod? = null
    private var selectedCardIndex: Int? = null

    private var didFireDismiss = false

    private lateinit var resumeTitle: TextView
    private lateinit var resumePhone: TextView
    private lateinit var resumeOffer: TextView
    private lateinit var resumePrice: TextView
    private lateinit var paymentSelectionContainer: LinearLayout
    private lateinit var scheduleContainer: LinearLayout
    private lateinit var scheduleSwitch: SwitchCompat
    private lateinit var scheduleTitleLabel: TextView
    private lateinit var confirmButton: DSSPrincipalButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply {
            setBackgroundColor(DSSColors.background())
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16f.dpToPx(ctx), 20f.dpToPx(ctx), 16f.dpToPx(ctx), 20f.dpToPx(ctx))
        }

        // Resume card section
        resumeTitle = TextView(ctx).apply {
            typeface = DSSFont.light(ctx, 22f).typeface
            textSize = 22f
            setTextColor(DSSColors.textPrimary())
            text = "Confirme sua recarga"
        }
        root.addView(resumeTitle, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        resumePhone = makeResumeLine(ctx, "Número")
        resumeOffer = makeResumeLine(ctx, "Oferta")
        resumePrice = makeResumeLine(ctx, "Valor")
        root.addView(resumePhone, resumeLineLp(ctx))
        root.addView(resumeOffer, resumeLineLp(ctx))
        root.addView(resumePrice, resumeLineLp(ctx))

        // Payment section
        val paymentSectionLabel = TextView(ctx).apply {
            text = "Pagamento"
            typeface = DSSFont.regular(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textSecondary())
        }
        root.addView(paymentSectionLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 20f.dpToPx(ctx) })

        paymentSelectionContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(paymentSelectionContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 12f.dpToPx(ctx) })

        // Schedule section
        scheduleContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        scheduleSwitch = SwitchCompat(ctx).apply {
            isChecked = true
            setOnCheckedChangeListener { _, on ->
                isScheduled = on
                delegate?.rechargeBottomSheetDidChangeScheduleStatus(this@DSSRechargeBottomSheet, on)
            }
        }
        scheduleTitleLabel = TextView(ctx).apply {
            text = "Programe suas recargas e ganhe bônus!"
            typeface = DSSFont.medium(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textPrimary())
        }
        scheduleContainer.addView(scheduleSwitch, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        scheduleContainer.addView(scheduleTitleLabel, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
        ).apply { leftMargin = 12f.dpToPx(ctx) })

        root.addView(scheduleContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 20f.dpToPx(ctx) })

        val scheduleDescriptionLabel = TextView(ctx).apply {
            text = "suas recargas são feitas automaticamente no cartão de crédito a cada 30 dias."
            typeface = DSSFont.regular(ctx, 12f).typeface
            textSize = 12f
            setTextColor(DSSColors.textSecondary())
        }
        root.addView(scheduleDescriptionLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 8f.dpToPx(ctx) })

        confirmButton = DSSPrincipalButton(ctx).apply {
            text = "Continuar"
            onTap = { confirmTapped() }
        }
        root.addView(confirmButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 48f.dpToPx(ctx),
        ).apply { topMargin = 20f.dpToPx(ctx) })

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))

        applyConfiguration()
        rebuildPaymentOptions()
        updateConfirmButtonState()
        updateScheduleVisibility()
        return scroll
    }

    private fun makeResumeLine(ctx: android.content.Context, label: String): TextView = TextView(ctx).apply {
        typeface = DSSFont.regular(ctx, 14f).typeface
        textSize = 14f
        setTextColor(DSSColors.textPrimary())
        text = "$label: "
    }

    private fun resumeLineLp(ctx: android.content.Context) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = 8f.dpToPx(ctx) }

    private fun applyConfiguration() {
        val cfg = configuration ?: return
        resumeTitle.text = cfg.title
        resumePhone.text = "Número: ${cfg.phoneNumber}"
        resumeOffer.text = "Oferta: ${cfg.offerName}"
        val reais = cfg.priceInCents / 100.0
        resumePrice.text = "Valor: R$ %.2f".format(reais)

        val bonus = cfg.scheduleBonus ?: "7GB"
        scheduleTitleLabel.text = "Programe suas recargas e ganhe até $bonus bônus!"
        if (!cfg.showScheduleOption || hideSchedule) {
            scheduleContainer.visibility = View.GONE
        }
    }

    private fun rebuildPaymentOptions() {
        paymentSelectionContainer.removeAllViews()
        val ctx = paymentSelectionContainer.context

        if (!showOnlyCards && showPix) {
            paymentSelectionContainer.addView(makePaymentRow(ctx, "PIX", PaymentMethod.PIX, cardIndex = null))
        }
        creditCards.forEachIndexed { index, _ ->
            paymentSelectionContainer.addView(
                makePaymentRow(ctx, "Cartão de crédito #${index + 1}", PaymentMethod.CREDIT_CARD, cardIndex = index),
            )
        }
        if (!showOnlyPix) {
            paymentSelectionContainer.addView(makePaymentRow(ctx, "+ Adicionar novo cartão", PaymentMethod.NEW_CARD, cardIndex = null))
        }
    }

    private fun makePaymentRow(
        ctx: android.content.Context, title: String, method: PaymentMethod, cardIndex: Int?,
    ): View {
        val tv = TextView(ctx).apply {
            text = title
            typeface = DSSFont.regular(ctx, 16f).typeface
            textSize = 16f
            setTextColor(DSSColors.textPrimary())
            setPadding(12f.dpToPx(ctx), 16f.dpToPx(ctx), 12f.dpToPx(ctx), 16f.dpToPx(ctx))
            isClickable = true
            setOnClickListener {
                when (method) {
                    PaymentMethod.NEW_CARD -> delegate?.rechargeBottomSheetDidTapAddCard(this@DSSRechargeBottomSheet)
                    PaymentMethod.PIX -> {
                        selectedMethod = PaymentMethod.PIX
                        selectedCardIndex = null
                    }
                    PaymentMethod.CREDIT_CARD -> {
                        selectedMethod = PaymentMethod.CREDIT_CARD
                        selectedCardIndex = cardIndex
                    }
                }
                updateConfirmButtonState()
                updateScheduleVisibility()
            }
        }
        return tv
    }

    private fun updateConfirmButtonState() {
        val hasSelection = selectedMethod != null && selectedMethod != PaymentMethod.NEW_CARD
        if (::confirmButton.isInitialized) {
            confirmButton.isEnabled = hasSelection
            confirmButton.alpha = if (hasSelection) 1.0f else 0.5f
        }
    }

    private fun updateScheduleVisibility() {
        val showScheduleOption = configuration?.showScheduleOption == true && !hideSchedule
        val shouldShow = selectedMethod != PaymentMethod.PIX && showScheduleOption
        if (::scheduleContainer.isInitialized) {
            scheduleContainer.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }
        if (!shouldShow) {
            scheduleSwitch.isChecked = true
            isScheduled = true
        }
    }

    private fun confirmTapped() {
        when (selectedMethod) {
            PaymentMethod.PIX -> delegate?.rechargeBottomSheetDidConfirmWithPix(this, isScheduled)
            PaymentMethod.CREDIT_CARD -> {
                val card = selectedCardIndex?.let { creditCards.getOrNull(it) }
                delegate?.rechargeBottomSheetDidConfirmWithCard(this, card, isScheduled)
            }
            else -> return
        }
        dismiss()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (!didFireDismiss) {
            didFireDismiss = true
            delegate?.rechargeBottomSheetDidDismiss(this)
        }
    }

    // MARK: - Public API

    fun configure(configuration: Configuration) {
        this.configuration = configuration
        if (::resumeTitle.isInitialized) applyConfiguration()
    }

    fun setCreditCards(cards: List<Any>) {
        this.creditCards = cards
        if (::paymentSelectionContainer.isInitialized) {
            rebuildPaymentOptions()
            updateConfirmButtonState()
            updateScheduleVisibility()
        }
    }

    fun setPixEnabled(enabled: Boolean, onlyPix: Boolean = false) {
        this.showPix = enabled
        this.showOnlyPix = onlyPix
        if (onlyPix) {
            selectedMethod = PaymentMethod.PIX
        }
        if (::paymentSelectionContainer.isInitialized) {
            rebuildPaymentOptions()
            updateConfirmButtonState()
            updateScheduleVisibility()
        }
    }

    fun setShowOnlyCards(onlyCards: Boolean) {
        this.showOnlyCards = onlyCards
        if (onlyCards && creditCards.isNotEmpty()) {
            selectedMethod = PaymentMethod.CREDIT_CARD
            selectedCardIndex = 0
        }
        if (::paymentSelectionContainer.isInitialized) {
            rebuildPaymentOptions()
            updateConfirmButtonState()
            updateScheduleVisibility()
        }
    }

    fun setConfirmButtonTitle(title: String) {
        if (::confirmButton.isInitialized) confirmButton.text = title
    }

    fun setScheduleSwitchOn(isOn: Boolean) {
        isScheduled = isOn
        if (::scheduleSwitch.isInitialized) scheduleSwitch.isChecked = isOn
    }

    fun resetSelection() {
        selectedMethod = null
        selectedCardIndex = null
        updateConfirmButtonState()
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            configuration: Configuration,
            cards: List<Any> = emptyList(),
            showPix: Boolean = false,
            showOnlyPix: Boolean = false,
            showOnlyCards: Boolean = false,
            hideSchedule: Boolean = false,
            delegate: Delegate? = null,
        ): DSSRechargeBottomSheet {
            val sheet = DSSRechargeBottomSheet()
            sheet.delegate = delegate
            sheet.configuration = configuration
            sheet.creditCards = cards
            sheet.showPix = showPix
            sheet.showOnlyPix = showOnlyPix
            sheet.showOnlyCards = showOnlyCards
            sheet.hideSchedule = hideSchedule
            sheet.show(activity.supportFragmentManager, "DSSRechargeBottomSheet")
            return sheet
        }
    }
}
