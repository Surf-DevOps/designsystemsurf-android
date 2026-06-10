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
 * Usa os componentes já portados [DSSResumeCard] e [DSSPaymentSelectionView] para
 * espelhar fielmente a estrutura e a lógica do iOS. A superfície pública (delegate,
 * `configure`, `setCreditCards`, etc) mantém `Any?`/`List<Any>` para os cartões a fim
 * de não quebrar a API existente; internamente apenas itens [DSSPaymentCard] são
 * encaminhados ao [DSSPaymentSelectionView].
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
    private var creditCards: List<DSSPaymentCard> = emptyList()
    private var showPix = false
    private var showOnlyPix = false
    private var showOnlyCards = false
    private var hideSchedule = false
    private var isScheduled = true

    private var didFireDismiss = false

    private lateinit var resumeCard: DSSResumeCard
    private lateinit var paymentSelectionView: DSSPaymentSelectionView
    private lateinit var scheduleContainer: LinearLayout
    private lateinit var scheduleSwitch: SwitchCompat
    private lateinit var scheduleTitleLabel: TextView
    private lateinit var confirmButton: DSSPrincipalButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply {
            setBackgroundColor(DSSColors.backgroundSecondary())
        }

        // iOS: contentStackView spacing = 20, alignment = .center, padding 16/20.
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16f.dpToPx(ctx), 20f.dpToPx(ctx), 16f.dpToPx(ctx), 20f.dpToPx(ctx))
        }

        // Resume card section — usa o DSSResumeCard portado (sem borda, igual ao iOS).
        resumeCard = DSSResumeCard(ctx).apply {
            borderWidthDp = 0f
            setCategoryLabels(number = "Número", offer = "Oferta", price = "Valor")
            // iOS: setTitle(... systemFont(22, .light), color .red); borderWidth = 0.
            setTitle("Confirme sua recarga")
            titleFont = DSSFont.light(ctx, 22f).typeface
        }
        root.addView(resumeCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

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

        paymentSelectionView = DSSPaymentSelectionView(ctx).apply {
            delegate = object : DSSPaymentSelectionViewDelegate {
                override fun didSelectPix(view: DSSPaymentSelectionView, card: DSSPaymentMethodCard) {
                    updateConfirmButtonState()
                    updateScheduleVisibility()
                }

                override fun didSelectNewCard(view: DSSPaymentSelectionView, card: DSSPaymentMethodCard) {
                    this@DSSRechargeBottomSheet.delegate?.rechargeBottomSheetDidTapAddCard(this@DSSRechargeBottomSheet)
                }

                override fun didSelectCreditCard(
                    view: DSSPaymentSelectionView,
                    card: DSSPaymentMethodCard,
                    cardData: DSSPaymentCard,
                    index: Int,
                ) {
                    updateConfirmButtonState()
                    updateScheduleVisibility()
                }

                override fun didDeselectPix(view: DSSPaymentSelectionView, card: DSSPaymentMethodCard) {
                    updateConfirmButtonState()
                    updateScheduleVisibility()
                }

                override fun didDeselectCreditCard(
                    view: DSSPaymentSelectionView,
                    card: DSSPaymentMethodCard,
                    cardData: DSSPaymentCard,
                    index: Int,
                ) {
                    updateConfirmButtonState()
                    updateScheduleVisibility()
                }
            }
        }
        // iOS: paymentContainer.heightAnchor.constraint(lessThanOrEqualToConstant: 300)
        root.addView(paymentSelectionView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 12f.dpToPx(ctx) })

        // Schedule section — switch + título na mesma linha, descrição abaixo.
        scheduleContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scheduleRow = LinearLayout(ctx).apply {
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
            // iOS default: "recharge_schedule.title_no_bonus"
            text = "Programe suas recargas e ganhe bônus!"
            typeface = DSSFont.medium(ctx, 14f).typeface
            textSize = 14f
            maxLines = 2
            setTextColor(DSSColors.textPrimary())
        }
        scheduleRow.addView(scheduleSwitch, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))
        scheduleRow.addView(scheduleTitleLabel, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
        ).apply { leftMargin = 12f.dpToPx(ctx) })
        scheduleContainer.addView(scheduleRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ))

        val scheduleDescriptionLabel = TextView(ctx).apply {
            text = "suas recargas são feitas automaticamente no cartão de crédito a cada 30 dias."
            typeface = DSSFont.regular(ctx, 12f).typeface
            textSize = 12f
            setTextColor(DSSColors.textSecondary())
        }
        scheduleContainer.addView(scheduleDescriptionLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 8f.dpToPx(ctx) })

        root.addView(scheduleContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 20f.dpToPx(ctx) })

        // iOS: confirmButton title = "recharge_schedule.continue", font regular(16), height 48.
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

        // iOS present(): resetSelection -> configure -> setCreditCards -> setPixEnabled
        // -> setShowOnlyCards -> updatePaymentMethodsVisibility -> hideSchedule.
        resetSelection()
        applyConfiguration()
        applyCreditCards()
        applyPixEnabled()
        applyShowOnlyCards()
        updatePaymentMethodsVisibility()
        if (hideSchedule) scheduleContainer.visibility = View.GONE

        updateConfirmButtonState()
        updateScheduleVisibility()
        return scroll
    }

    private fun applyConfiguration() {
        val cfg = configuration ?: return
        resumeCard.setTitle(cfg.title)
        resumeCard.configure(
            number = cfg.phoneNumber,
            offer = cfg.offerName,
            priceInCents = cfg.priceInCents,
        )

        // iOS: scheduleSwitchContainer.isHidden = !showScheduleOption (em configure).
        scheduleContainer.visibility =
            if (cfg.showScheduleOption) View.VISIBLE else View.GONE

        // iOS: bonus vazio ou "0GB" -> title_no_bonus; senão title_with_bonus(bonus).
        val bonus = cfg.scheduleBonus ?: ""
        scheduleTitleLabel.text = if (bonus.isEmpty() || bonus == "0GB") {
            "Programe suas recargas e ganhe bônus!"
        } else {
            "Programe suas recargas e ganhe até $bonus de bônus!"
        }
    }

    /** Encaminha os cartões filtrados ao [DSSPaymentSelectionView] e aplica pré-seleções. */
    private fun applyCreditCards() {
        paymentSelectionView.setCreditCards(creditCards)
        // iOS: cards.isEmpty && showPix -> select .pix
        if (creditCards.isEmpty() && showPix) {
            paymentSelectionView.setSelectedPaymentMethod(DSSPaymentMethod.Pix)
        }
        updateConfirmButtonState()
        updateScheduleVisibility()
    }

    private fun applyPixEnabled() {
        if (showOnlyPix) {
            paymentSelectionView.setSelectedPaymentMethod(DSSPaymentMethod.Pix)
            updatePaymentMethodsVisibility()
        }
        // iOS: creditCards.isEmpty && enabled -> select .pix
        if (creditCards.isEmpty() && showPix) {
            paymentSelectionView.setSelectedPaymentMethod(DSSPaymentMethod.Pix)
        }
        updateConfirmButtonState()
        updateScheduleVisibility()
    }

    private fun applyShowOnlyCards() {
        updatePaymentMethodsVisibility()
        // iOS: onlyCards && !creditCards.isEmpty -> select .creditCard(index: 0)
        if (showOnlyCards && creditCards.isNotEmpty()) {
            paymentSelectionView.setSelectedPaymentMethod(DSSPaymentMethod.CreditCard(0))
        }
        updateConfirmButtonState()
        updateScheduleVisibility()
    }

    private fun updateConfirmButtonState() {
        if (!::confirmButton.isInitialized || !::paymentSelectionView.isInitialized) return
        // iOS: getSelectedPaymentMethod() != nil
        val hasSelection = paymentSelectionView.getSelectedPaymentMethod() != null
        confirmButton.isEnabled = hasSelection
        confirmButton.alpha = if (hasSelection) 1.0f else 0.5f
    }

    private fun updateScheduleVisibility() {
        if (!::scheduleContainer.isInitialized || !::paymentSelectionView.isInitialized) return
        val isPixSelected = paymentSelectionView.getSelectedPaymentMethod() == DSSPaymentMethod.Pix
        // iOS: shouldShow = !isPixSelected && configuration?.showScheduleOption == true
        val shouldShow = !isPixSelected && configuration?.showScheduleOption == true && !hideSchedule
        scheduleContainer.visibility = if (shouldShow) View.VISIBLE else View.GONE
        if (!shouldShow) {
            scheduleSwitch.isChecked = true
            isScheduled = true
        }
    }

    private fun confirmTapped() {
        val isScheduled = this.isScheduled
        when (val selected = paymentSelectionView.getSelectedPaymentMethod()) {
            DSSPaymentMethod.Pix ->
                delegate?.rechargeBottomSheetDidConfirmWithPix(this, isScheduled)
            is DSSPaymentMethod.CreditCard -> {
                paymentSelectionView.getSelectedCreditCardData()?.let { cardData ->
                    delegate?.rechargeBottomSheetDidConfirmWithCard(this, cardData, isScheduled)
                }
            }
            // .newCard / null -> não confirma (iOS: break / guard).
            else -> Unit
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

    /** Configura o BottomSheet com os dados da recarga. */
    fun configure(configuration: Configuration) {
        this.configuration = configuration
        if (::resumeCard.isInitialized) applyConfiguration()
    }

    /**
     * Define os cartões de crédito disponíveis. Mantém a assinatura `List<Any>` da API
     * existente; somente itens [DSSPaymentCard] são encaminhados ao seletor.
     */
    fun setCreditCards(cards: List<Any>) {
        this.creditCards = cards.filterIsInstance<DSSPaymentCard>()
        if (::paymentSelectionView.isInitialized) applyCreditCards()
    }

    /** Habilita/desabilita a opção de PIX. */
    fun setPixEnabled(enabled: Boolean, onlyPix: Boolean = false) {
        this.showPix = enabled
        this.showOnlyPix = onlyPix
        if (::paymentSelectionView.isInitialized) applyPixEnabled()
    }

    /** Define se deve mostrar apenas cartões. */
    fun setShowOnlyCards(onlyCards: Boolean) {
        this.showOnlyCards = onlyCards
        if (::paymentSelectionView.isInitialized) applyShowOnlyCards()
    }

    /**
     * Atualiza a visibilidade dos métodos de pagamento (PIX / novo cartão) com base
     * nas flags `showPix`, `showOnlyPix` e `showOnlyCards`. Público no iOS.
     */
    fun updatePaymentMethodsVisibility() {
        if (!::paymentSelectionView.isInitialized) return
        if (showOnlyCards) {
            paymentSelectionView.setPixVisibility(false)
        } else {
            paymentSelectionView.setPixVisibility(showPix)
        }

        if (showOnlyPix) {
            paymentSelectionView.setNewCardVisibility(false)
        } else {
            paymentSelectionView.setNewCardVisibility(true)
        }
    }

    /** Define o texto do botão de confirmação. */
    fun setConfirmButtonTitle(title: String) {
        if (::confirmButton.isInitialized) confirmButton.text = title
    }

    /** Define o estado do switch de agendamento. */
    fun setScheduleSwitchOn(isOn: Boolean) {
        if (::scheduleSwitch.isInitialized) scheduleSwitch.isChecked = isOn
        isScheduled = isOn
    }

    /** Reseta o estado de seleção. */
    fun resetSelection() {
        if (::paymentSelectionView.isInitialized) {
            paymentSelectionView.setSelectedPaymentMethod(null)
            updateConfirmButtonState()
        }
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
            sheet.creditCards = cards.filterIsInstance<DSSPaymentCard>()
            sheet.showPix = showPix
            sheet.showOnlyPix = showOnlyPix
            sheet.showOnlyCards = showOnlyCards
            sheet.hideSchedule = hideSchedule
            sheet.show(activity.supportFragmentManager, "DSSRechargeBottomSheet")
            return sheet
        }
    }
}
