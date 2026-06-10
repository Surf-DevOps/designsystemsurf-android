package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.transition.TransitionManager
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Port do `DSSMyPlanScheduleView.swift` (iOS) — view do "Meu plano" com card de oferta,
 * botões de ação (Trocar plano / Alterar cartão / Cancelar) e calendário de antecipação.
 */
class DSSMyPlanScheduleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    interface Delegate {
        fun onTrocarPlano(view: DSSMyPlanScheduleView)
        fun onAlterarCartao(view: DSSMyPlanScheduleView)
        fun onCancelarProgramada(view: DSSMyPlanScheduleView)
        fun onSaveAnticipationDateIso(view: DSSMyPlanScheduleView, isoDate: String)
    }

    var delegate: Delegate? = null

    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayFormatter = SimpleDateFormat("dd MMM. yyyy", Locale("pt", "BR"))

    private var isAnticipationMode = false
    private var backendDate: Date? = null

    private val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    // Offer card
    private val offerCard = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val headerLabel = TextView(context).apply {
        text = "Sua oferta"; textSize = 16f
        typeface = DSSFont.light(context, 16f).typeface
    }
    private val planNameLabel = TextView(context).apply { textSize = 20f; typeface = DSSFont.bold(context, 20f).typeface }
    private val priceLabel = TextView(context).apply { gravity = Gravity.END }
    private val renewalBadge = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 13f
        typeface = DSSFont.light(context, 13f).typeface
        setPadding(16f.dpToPx(context), 4f.dpToPx(context), 16f.dpToPx(context), 4f.dpToPx(context))
    }
    private val anteciparLink = TextView(context).apply {
        text = "Antecipar"
        textSize = 14f
        typeface = DSSFont.light(context, 14f).typeface
        isClickable = true
        isFocusable = true
    }
    private val dateLabel = TextView(context).apply { textSize = 14f; typeface = DSSFont.light(context, 14f).typeface }

    // Action buttons
    private val trocarPlanoCard = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val alterarCartaoCard = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val cancelarCard = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val trocarPlanoButton = TextView(context).apply {
        text = "Trocar plano"
        textSize = 16f; typeface = DSSFont.regular(context, 16f).typeface
        gravity = Gravity.CENTER
        isClickable = true; isFocusable = true
    }
    private val alterarCartaoButton = TextView(context).apply {
        text = "Alterar cartão"
        textSize = 16f; typeface = DSSFont.regular(context, 16f).typeface
        gravity = Gravity.CENTER
        isClickable = true; isFocusable = true
    }
    private val cancelarButton = TextView(context).apply {
        text = "Cancelar programada"
        textSize = 16f; typeface = DSSFont.regular(context, 16f).typeface
        gravity = Gravity.CENTER
        isClickable = true; isFocusable = true
    }

    // Calendar container
    private val calendarContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
    }
    private val calendarGrid = DSSScheduleCalendarView(context)
    private val saveButton = DSSPrincipalButton(context).apply { text = "Salvar data" }

    init {
        column.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(column)

        setupOfferCard()
        setupActionCard(trocarPlanoCard, trocarPlanoButton)
        setupActionCard(alterarCartaoCard, alterarCartaoButton)
        setupActionCard(cancelarCard, cancelarButton)
        setupCalendarContainer()

        val between = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 8f.dpToPx(context) }

        column.addView(offerCard)
        column.addView(calendarContainer, between)
        column.addView(trocarPlanoCard, between)
        column.addView(alterarCartaoCard, between)
        column.addView(cancelarCard, between)

        anteciparLink.setOnClickListener { toggleAnticipationMode() }
        trocarPlanoButton.setOnClickListener { delegate?.onTrocarPlano(this) }
        alterarCartaoButton.setOnClickListener { delegate?.onAlterarCartao(this) }
        cancelarButton.setOnClickListener { delegate?.onCancelarProgramada(this) }
        saveButton.onTap = { saveTapped() }

        refresh()
        setupThemeObserver()
    }

    private fun setupOfferCard() {
        val pad = 20f.dpToPx(context)
        val hPad = 24f.dpToPx(context)
        offerCard.setPadding(hPad, pad, hPad, pad)

        offerCard.addView(headerLabel)

        val nameRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        nameRow.addView(planNameLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        nameRow.addView(priceLabel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        offerCard.addView(
            nameRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 8f.dpToPx(context) },
        )

        val badgeRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        badgeRow.addView(renewalBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 28f.dpToPx(context)))
        badgeRow.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        badgeRow.addView(anteciparLink)
        offerCard.addView(
            badgeRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 12f.dpToPx(context) },
        )

        offerCard.addView(
            dateLabel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 4f.dpToPx(context) },
        )
    }

    private fun setupActionCard(card: LinearLayout, button: TextView) {
        val hPad = 24f.dpToPx(context); val vPad = 12f.dpToPx(context)
        card.setPadding(hPad, vPad, hPad, vPad)
        card.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private fun setupCalendarContainer() {
        val hPad = 16f.dpToPx(context); val vPad = 16f.dpToPx(context)
        calendarContainer.setPadding(hPad, vPad, hPad, vPad)
        calendarContainer.addView(
            calendarGrid,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        calendarContainer.addView(
            saveButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50f.dpToPx(context),
            ).apply { topMargin = 16f.dpToPx(context); leftMargin = 8f.dpToPx(context); rightMargin = 8f.dpToPx(context) },
        )
    }

    private fun toggleAnticipationMode() {
        isAnticipationMode = !isAnticipationMode
        anteciparLink.text = if (isAnticipationMode) "Manter data" else "Antecipar"
        TransitionManager.beginDelayedTransition(this)
        calendarContainer.visibility = if (isAnticipationMode) View.VISIBLE else View.GONE
        trocarPlanoCard.visibility = if (isAnticipationMode) View.GONE else View.VISIBLE
        alterarCartaoCard.visibility = if (isAnticipationMode) View.GONE else View.VISIBLE
        cancelarCard.visibility = if (isAnticipationMode) View.GONE else View.VISIBLE
        if (!isAnticipationMode) {
            backendDate?.let { dateLabel.text = displayFormatter.format(it) }
        }
    }

    private fun saveTapped() {
        val selected = calendarGrid.selectedDate ?: return
        dateLabel.text = displayFormatter.format(selected)
        delegate?.onSaveAnticipationDateIso(this, isoFormatter.format(selected))
        // Exit anticipation mode
        isAnticipationMode = false
        anteciparLink.text = "Antecipar"
        TransitionManager.beginDelayedTransition(this)
        calendarContainer.visibility = View.GONE
        trocarPlanoCard.visibility = View.VISIBLE
        alterarCartaoCard.visibility = View.VISIBLE
        cancelarCard.visibility = View.VISIBLE
    }

    /**
     * Configura o conteúdo do card de oferta e o range do calendário.
     */
    fun configure(
        planName: String,
        priceCents: Int,
        renewalDateIso: String,
        badgeText: String = "próxima renovação",
    ) {
        planNameLabel.text = planName
        renewalBadge.text = badgeText
        val date = try { isoFormatter.parse(renewalDateIso) } catch (_: Throwable) { null }
        if (date != null) {
            backendDate = date
            dateLabel.text = displayFormatter.format(date)
            // iOS: daysBack = max(0, dias entre o início de hoje e o início do dia da renovação).
            val today = startOfDay(Date())
            val renewalDay = startOfDay(date)
            val diffMs = renewalDay.time - today.time
            val daysBack = maxOf(0L, diffMs / MILLIS_PER_DAY).toInt()
            calendarGrid.configure(maxDate = date, daysBack = daysBack)
        } else {
            dateLabel.text = renewalDateIso
        }
        val reais = priceCents / 100
        priceLabel.text = "R$$reais/mês"
        priceLabel.typeface = DSSFont.bold(context, 16f).typeface
        priceLabel.textSize = 16f
    }

    fun setAnteciparVisible(visible: Boolean) {
        anteciparLink.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setRenewalBadgeVisible(visible: Boolean) {
        renewalBadge.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        val cardBg = DSSColors.backgroundSecondary()
        listOf(offerCard, trocarPlanoCard, alterarCartaoCard, cancelarCard, calendarContainer).forEach { card ->
            card.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = cardBg,
                cornerRadiusDp = 16f,
            )
        }
        headerLabel.setTextColor(DSSColors.textSecondary())
        planNameLabel.setTextColor(DSSColors.textPrimary())
        priceLabel.setTextColor(DSSColors.textPrimary())
        dateLabel.setTextColor(DSSColors.textPrimary())
        renewalBadge.background = DrawableFactory.rounded(
            context = context,
            backgroundColor = DSSColors.primary(),
            cornerRadiusDp = 14f,
        )
        renewalBadge.setTextColor(DSSColors.textOnPrimary())
        anteciparLink.setTextColor(DSSColors.textLink())
        trocarPlanoButton.setTextColor(DSSColors.textPrimary())
        alterarCartaoButton.setTextColor(DSSColors.textPrimary())
        cancelarButton.setTextColor(DSSColors.error())
    }

    /** Zera hora/minuto/segundo/ms — equivalente a `Calendar.startOfDay` do iOS. */
    private fun startOfDay(date: Date): Date {
        val c = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.time
    }

    private companion object {
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
