package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Port do `DSSScheduledCardBottomSheet.swift` (iOS) — fluxo "Cartão da programada".
 *
 * Contém dois bottom sheets:
 *  1. [DSSScheduledCardBottomSheet] -> lista os cartões e destaca o cartão da programada
 *     (com recorrência ativa) exibindo a data, valor, plano e telefone. Ao confirmar a
 *     troca, dispara o callback de confirmação e abre o sheet de sucesso.
 *  2. [DSSScheduledCardSuccessBottomSheet] -> confirmação "Cartão da programada alterado com sucesso!".
 *
 * O componente NÃO faz `listCards`/`listRecurrences` — apenas recebe os dados já carregados
 * pelo app via [present] / [setData] / [setCards]. A única chamada de API (troca do cartão)
 * fica a cargo do app, exposta aqui pelo callback [Delegate.scheduledCardBottomSheetDidConfirm]
 * (ou [onConfirm]); o app deve chamar [showSuccess]/[setConfirmLoading] conforme o resultado.
 */
class DSSScheduledCardBottomSheet : BottomSheetDialogFragment() {

    // MARK: - Modelos

    /** Cartão exibido na lista. Espelha o `Card` do iOS (campos usados pelo componente). */
    data class CardModel(
        val cardId: String,
        val lastFour: String?,
        val flag: String?,
        val isDefault: Boolean,
        /** Quando não-nulo, o cartão é "da programada" (possui recorrência ativa). */
        val hasRecurrence: Boolean,
    )

    /** Recorrência ativa — espelha o `ListRecurrencesSuccess` do iOS (campos usados). */
    data class RecurrenceModel(
        val id: String?,
        val nextExecutionAt: String?,
        val planName: String?,
        val planValueReais: Double?,
        val ddd: String?,
        val msisdn: String?,
    )

    // MARK: - Delegate / callbacks

    interface Delegate {
        /** Chamado ao confirmar a alteração do cartão da programada. */
        fun scheduledCardBottomSheetDidConfirm(sheet: DSSScheduledCardBottomSheet, card: CardModel)
        /** Chamado ao tocar em "Adicionar cartão". */
        fun scheduledCardBottomSheetDidTapAddCard(sheet: DSSScheduledCardBottomSheet) {}
    }

    var delegate: Delegate? = null

    /** Handler chamado ao confirmar a troca. Use para chamar sua API de troca de cartão. */
    var onConfirm: ((CardModel) -> Unit)? = null

    /** Handler chamado ao tocar em "Adicionar cartão". */
    var onAddCard: (() -> Unit)? = null

    /**
     * Resolver de bandeira do cartão (ilVisa / ilElo / ilMaster). Recebe o nome lower-case
     * do recurso e devolve o Drawable da brand. Se nulo, faz lookup via [ImageLoader]
     * (espelha o `ImageLoader.image(named:brand:)` do iOS).
     */
    var cardImageResolver: ((String) -> Drawable?)? = null

    /** Drawable da ilustração de sucesso, provido pelo módulo de brand. */
    var illustration: Drawable? = null

    // MARK: - Estado

    private var recurrence: RecurrenceModel? = null
    private var cards: List<CardModel> = emptyList()
    private var selectedIndex: Int? = null

    private var hostActivity: FragmentActivity? = null

    // MARK: - Views

    private var recyclerView: RecyclerView? = null
    private var adapter: CardAdapter? = null
    private var messageLabel: TextView? = null
    private var confirmButton: DSSPrincipalButton? = null
    private var confirmProgress: ProgressBar? = null
    private var confirmContainer: FrameLayout? = null

    // MARK: - Lifecycle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        // iOS: containerView cornerRadius 24 com maskedCorners nos cantos superiores.
        val scroll = ScrollView(ctx).apply { background = roundedTopBackground(ctx, DSSColors.background()) }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24f.dpToPx(ctx), 12f.dpToPx(ctx), 24f.dpToPx(ctx), 20f.dpToPx(ctx))
        }

        // Handle (40x5, cantos 2.5, systemGray4) — topo 12
        val handle = View(ctx).apply {
            background = DrawableFactory.rounded(
                context = ctx, backgroundColor = DSSColors.divider(), cornerRadiusDp = 2.5f,
            )
        }
        root.addView(handle, LinearLayout.LayoutParams(
            40f.dpToPx(ctx), 5f.dpToPx(ctx),
        ).apply { gravity = Gravity.CENTER_HORIZONTAL })

        // Título "Cartão da programada"
        val titleLabel = TextView(ctx).apply {
            text = "Cartão da programada"
            typeface = DSSFont.light(ctx, 24f).typeface
            textSize = 24f
            setTextColor(DSSColors.textPrimary())
        }
        root.addView(titleLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 28f.dpToPx(ctx) })

        // Lista de cartões
        val rv = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            isNestedScrollingEnabled = false
        }
        recyclerView = rv
        root.addView(rv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24f.dpToPx(ctx) })

        // Mensagem (estado vazio)
        val message = TextView(ctx).apply {
            typeface = DSSFont.regular(ctx, 14f).typeface
            textSize = 14f
            setTextColor(DSSColors.textSecondary())
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        messageLabel = message
        root.addView(message, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 8f.dpToPx(ctx) })

        // Botão de confirmação (com indicador de loading sobreposto)
        val btnContainer = FrameLayout(ctx)
        confirmContainer = btnContainer
        val confirm = DSSPrincipalButton(ctx).apply {
            text = "Confirmar alteração"
            onTap = { confirmTapped() }
        }
        confirmButton = confirm
        btnContainer.addView(confirm, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ))
        val progress = ProgressBar(ctx).apply {
            isIndeterminate = true
            visibility = View.GONE
            indeterminateTintList = android.content.res.ColorStateList.valueOf(DSSColors.buttonText())
        }
        confirmProgress = progress
        btnContainer.addView(progress, FrameLayout.LayoutParams(
            24f.dpToPx(ctx), 24f.dpToPx(ctx), Gravity.CENTER,
        ))
        root.addView(btnContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 50f.dpToPx(ctx),
        ).apply { topMargin = 28f.dpToPx(ctx) })

        // Botão "Adicionar cartão"
        val addCard = AppCompatButton(ctx).apply {
            text = "Adicionar cartão"
            isAllCaps = false
            typeface = DSSFont.regular(ctx, 16f).typeface
            textSize = 16f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(DSSColors.primary())
            setOnClickListener { addCardTapped() }
        }
        root.addView(addCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = 16f.dpToPx(ctx)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))

        adapter = CardAdapter()
        rv.adapter = adapter

        renderData()
        return scroll
    }

    // MARK: - API pública

    /** Atualiza a recorrência e os cartões exibidos (ex.: após o app recarregar os dados). */
    fun setData(recurrence: RecurrenceModel?, cards: List<CardModel>) {
        this.recurrence = recurrence
        this.cards = cards
        messageLabel?.visibility = View.GONE
        renderData()
    }

    /** Atualiza apenas os cartões exibidos. */
    fun setCards(cards: List<CardModel>) {
        this.cards = cards
        messageLabel?.visibility = View.GONE
        renderData()
    }

    /** Liga/desliga o estado de carregamento do botão de confirmação. */
    fun setConfirmLoading(loading: Boolean) {
        val confirm = confirmButton ?: return
        val progress = confirmProgress ?: return
        if (loading) {
            isCancelable = false
            confirm.isEnabled = false
            confirm.text = ""
            progress.visibility = View.VISIBLE
        } else {
            isCancelable = true
            confirm.isEnabled = true
            confirm.text = "Confirmar alteração"
            progress.visibility = View.GONE
        }
    }

    /** Fecha este sheet e abre o sheet de sucesso. Chame ao concluir a troca com sucesso. */
    fun showSuccess() {
        val host = hostActivity ?: activity as? FragmentActivity
        dismissAllowingStateLoss()
        host?.let { DSSScheduledCardSuccessBottomSheet.present(it, illustration = illustration) }
    }

    // MARK: - Render

    private fun renderData() {
        if (cards.isEmpty()) {
            showMessage("Nenhum cartão cadastrado.")
            return
        }
        // Pré-seleciona o cartão da programada (com recorrência) ou o default.
        val scheduledIndex = cards.indexOfFirst { it.hasRecurrence }
        selectedIndex = when {
            scheduledIndex >= 0 -> scheduledIndex
            cards.indexOfFirst { it.isDefault } >= 0 -> cards.indexOfFirst { it.isDefault }
            else -> 0
        }
        showRows()
    }

    private fun showMessage(text: String) {
        recyclerView?.visibility = View.GONE
        confirmButton?.isEnabled = false
        confirmButton?.alpha = 0.5f
        messageLabel?.text = text
        messageLabel?.visibility = View.VISIBLE
    }

    private fun showRows() {
        recyclerView?.visibility = View.VISIBLE
        confirmButton?.isEnabled = true
        confirmButton?.alpha = 1.0f
        messageLabel?.visibility = View.GONE
        adapter?.submit(displayOrder())
    }

    /**
     * Índices dos cartões na ordem de exibição: só o cartão da programada (recorrência ativa)
     * pode ficar em primeiro; os demais mantêm a ordem original.
     */
    private fun displayOrder(): List<Int> {
        val all = cards.indices.toList()
        val programadaIndex = cards.indexOfFirst { it.hasRecurrence }
        if (programadaIndex < 0) return all
        return listOf(programadaIndex) + all.filter { it != programadaIndex }
    }

    private fun onRowTapped(cardIndex: Int) {
        if (cardIndex >= cards.size || selectedIndex == cardIndex) return
        selectedIndex = cardIndex
        adapter?.notifyDataSetChanged()
    }

    // MARK: - Helpers

    private fun isProgramada(card: CardModel): Boolean = card.hasRecurrence

    /** Data da programada (`nextExecutionAt`) formatada como dd/MM/aa. */
    private fun scheduledDateText(): String? {
        val iso = recurrence?.nextExecutionAt
        if (iso.isNullOrEmpty()) return null
        val date: Date? = isoFormatter.parse(iso) ?: isoFormatterNoFraction.parse(iso)
        return if (date != null) dateDisplayFormatter.format(date) else iso
    }

    /** Valor da programada (`planValueReais`) formatado em BRL. */
    private fun valueText(): String? {
        val value = recurrence?.planValueReais ?: return null
        return brlFormatter.format(value)
    }

    /** Telefone derivado da recorrência (ddd + msisdn) no formato (DD) 9XXXX-XXXX. */
    private fun phoneText(): String? {
        val rec = recurrence ?: return null
        val ddd = (rec.ddd ?: "").filter { it.isDigit() }
        val number = (rec.msisdn ?: "").filter { it.isDigit() }
        val raw = if (number.length <= 9) ddd + number else number
        return formatBrazilianPhone(raw)
    }

    /**
     * Espelha o pipeline do iOS `String.normalizeBrazilianPhone(_:)?.formatPhoneNumber()`.
     * Primeiro normaliza para E.164 BR (55 + DDD + número, 12 ou 13 dígitos; nil caso
     * contrário) e só então formata `(DD) 9XXXX-XXXX` quando há exatamente 11 dígitos
     * após o "55"; nos demais casos devolve a string normalizada inalterada (como o iOS).
     */
    private fun formatBrazilianPhone(raw: String): String? {
        val normalized = normalizeBrazilianPhone(raw) ?: return null
        return formatPhoneNumber(normalized)
    }

    /** Equivalente ao `String.normalizeBrazilianPhone(_:)` do iOS. */
    private fun normalizeBrazilianPhone(raw: String): String? {
        val digits = raw.trim().filter { it.isDigit() }
        if (digits.isEmpty()) return null

        var cleaned = digits
        // Remove prefixo internacional com zeros (ex.: 0055..., 00055...) e zeros à esquerda.
        if (cleaned.startsWith("00")) cleaned = cleaned.dropWhile { it == '0' }
        while (cleaned.startsWith("0")) cleaned = cleaned.substring(1)

        // Garante prefixo do Brasil.
        if (!cleaned.startsWith("55")) cleaned = "55$cleaned"

        // E.164 BR: 55 + DDD(2) + número(8 ou 9) => 12 ou 13 dígitos.
        return if (cleaned.length == 12 || cleaned.length == 13) cleaned else null
    }

    /** Equivalente ao `String.formatPhoneNumber()` do iOS. */
    private fun formatPhoneNumber(value: String): String {
        if (!value.startsWith("55")) return value
        val trimmed = value.substring(2)
        if (trimmed.length != 11) return value
        val ddd = trimmed.substring(0, 2)
        val prefix = trimmed.substring(2, 7)
        val suffix = trimmed.substring(7)
        return "($ddd) $prefix-$suffix"
    }

    private fun resolveCardImage(card: CardModel): Drawable? {
        val flag = (card.flag ?: "").lowercase()
        val name = when {
            flag.contains("visa") -> "ilvisa"
            flag.contains("elo") -> "ilelo"
            else -> "ilmaster"
        }
        cardImageResolver?.invoke(name)?.let { return it }
        // iOS: ImageLoader.image(named:brand:) com BrandResolver.current().
        val ctx = context ?: return null
        return ImageLoader.image(ctx, name)
    }

    // MARK: - Actions

    private fun confirmTapped() {
        val index = selectedIndex ?: return
        if (index >= cards.size) return
        val selectedCard = cards[index]

        // Se o cartão selecionado já é o da programada, não há troca a fazer.
        if (selectedCard.hasRecurrence) {
            showError("Este cartão já é o cartão da programada.")
            return
        }
        val recurrenceId = recurrence?.id
        if (recurrenceId.isNullOrEmpty()) {
            showError("Não foi possível identificar a programada.")
            return
        }

        // Notifica o app para executar a troca. O app deve chamar showSuccess()
        // (ou setConfirmLoading(false) + showError) conforme o resultado da API.
        delegate?.scheduledCardBottomSheetDidConfirm(this, selectedCard)
        onConfirm?.invoke(selectedCard)
    }

    /** Exibe uma mensagem de erro simples. */
    fun showError(message: String) {
        val ctx = context ?: return
        android.app.AlertDialog.Builder(ctx)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun addCardTapped() {
        delegate?.scheduledCardBottomSheetDidTapAddCard(this)
        onAddCard?.invoke()
    }

    // MARK: - Adapter

    private inner class CardAdapter : RecyclerView.Adapter<CardAdapter.Holder>() {

        private var order: List<Int> = emptyList()

        fun submit(order: List<Int>) {
            this.order = order
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = order.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val row = DSSScheduledCardRow(parent.context)
            row.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT,
            )
            return Holder(row)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val cardIndex = order[position]
            val card = cards[cardIndex]
            val row = holder.row
            (row.layoutParams as? RecyclerView.LayoutParams)?.topMargin =
                if (position == 0) 0 else 16f.dpToPx(row.context)
            row.configure(
                card = card,
                isSelected = selectedIndex == cardIndex,
                isProgramada = isProgramada(card),
                scheduledDateText = scheduledDateText(),
                valueText = valueText(),
                planName = recurrence?.planName,
                phone = phoneText(),
                cardImage = resolveCardImage(card),
            )
            row.setOnClickListener { onRowTapped(cardIndex) }
        }

        inner class Holder(val row: DSSScheduledCardRow) : RecyclerView.ViewHolder(row)
    }

    // MARK: - Card Row

    private class DSSScheduledCardRow(context: Context) : FrameLayout(context) {

        private val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

        private val cardImageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            // iOS: cardImageView.layer.cornerRadius = 6 + clipsToBounds = true
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 6f.dpToPx(context).toFloat())
                }
            }
        }
        private val titleLabel = TextView(context).apply {
            text = "Cartão cadastrado"
            typeface = DSSFont.medium(context, 14f).typeface
            textSize = 14f
        }
        private val lastFourLabel = TextView(context).apply {
            typeface = DSSFont.regular(context, 14f).typeface
            textSize = 14f
        }
        private val statusLabel = TextView(context).apply {
            typeface = DSSFont.medium(context, 14f).typeface
            textSize = 14f
            gravity = Gravity.END
        }
        private val planTagLabel = paddingLabel(context).apply {
            typeface = DSSFont.regular(context, 12f).typeface
            textSize = 12f
        }
        private val scheduledBadgeLabel = paddingLabel(context).apply {
            typeface = DSSFont.medium(context, 12f).typeface
            textSize = 12f
            // contraste sobre o fill primary: branco em primary saturado (=iOS), escuro
            // quando o primary vira branco no dark/black -> não some no badge.
            setTextColor(DSSColors.contrastOnPrimary())
            gravity = Gravity.CENTER
        }
        private val phoneLabel = TextView(context).apply {
            typeface = DSSFont.regular(context, 14f).typeface
            textSize = 14f
        }
        private val valueLabel = TextView(context).apply {
            typeface = DSSFont.bold(context, 16f).typeface
            textSize = 16f
            gravity = Gravity.END
        }

        private val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        private val titleStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        private val tagBadgeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        private val phoneValueRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        init {
            // header: imagem + (título / final) + status
            headerRow.addView(cardImageView, LinearLayout.LayoutParams(
                56f.dpToPx(context), 36f.dpToPx(context),
            ))
            titleStack.addView(titleLabel)
            titleStack.addView(lastFourLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 4f.dpToPx(context) })
            headerRow.addView(titleStack, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
            ).apply { leftMargin = 16f.dpToPx(context); rightMargin = 16f.dpToPx(context) })
            headerRow.addView(statusLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
            container.addView(headerRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ))

            // tag de plano + spacer + badge da programada
            tagBadgeRow.addView(planTagLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
            tagBadgeRow.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
            tagBadgeRow.addView(scheduledBadgeLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 12f.dpToPx(context) })
            container.addView(tagBadgeRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 16f.dpToPx(context) })

            // telefone + spacer + valor
            phoneValueRow.addView(phoneLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ))
            phoneValueRow.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
            phoneValueRow.addView(valueLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { leftMargin = 12f.dpToPx(context) })
            container.addView(phoneValueRow, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 16f.dpToPx(context) })

            val pad = 18f.dpToPx(context)
            container.setPadding(pad, pad, pad, pad)
            addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

        fun configure(
            card: CardModel,
            isSelected: Boolean,
            isProgramada: Boolean,
            scheduledDateText: String?,
            valueText: String?,
            planName: String?,
            phone: String?,
            cardImage: Drawable?,
        ) {
            lastFourLabel.text = "Final ${card.lastFour ?: "****"}"
            cardImageView.setImageDrawable(cardImage)

            // Status: selecionado -> "Selecionado" (verde);
            // cartão da programada -> "Utilizando"; demais -> "Utilizar cartão".
            when {
                isSelected -> {
                    statusLabel.text = "Selecionado"
                    statusLabel.setTextColor(DSSColors.success())
                }
                isProgramada -> {
                    statusLabel.text = "Utilizando"
                    statusLabel.setTextColor(DSSColors.textSecondary())
                }
                else -> {
                    statusLabel.text = "Utilizar cartão"
                    statusLabel.setTextColor(DSSColors.primary())
                }
            }

            // O cartão "abre" quando selecionado.
            val expanded = isSelected

            // Tag de plano
            if (expanded && !planName.isNullOrEmpty()) {
                planTagLabel.text = planName
                planTagLabel.visibility = View.VISIBLE
            } else {
                planTagLabel.visibility = View.GONE
            }

            // Badge da programada com a data — só no cartão selecionado E com recorrência.
            if (expanded && isProgramada && !scheduledDateText.isNullOrEmpty()) {
                scheduledBadgeLabel.text = "Programada: $scheduledDateText"
                scheduledBadgeLabel.visibility = View.VISIBLE
            } else {
                scheduledBadgeLabel.visibility = View.GONE
            }

            // Telefone
            if (expanded && !phone.isNullOrEmpty()) {
                phoneLabel.text = phone
                phoneLabel.visibility = View.VISIBLE
            } else {
                phoneLabel.visibility = View.GONE
            }

            // Valor
            if (expanded && valueText != null) {
                valueLabel.text = valueText
                valueLabel.visibility = View.VISIBLE
            } else {
                valueLabel.visibility = View.GONE
            }

            // Esconde linhas vazias (cartão "fechado")
            tagBadgeRow.visibility =
                if (planTagLabel.visibility == View.GONE && scheduledBadgeLabel.visibility == View.GONE)
                    View.GONE else View.VISIBLE
            phoneValueRow.visibility =
                if (phoneLabel.visibility == View.GONE && valueLabel.visibility == View.GONE)
                    View.GONE else View.VISIBLE

            applyStyle(isSelected)
        }

        private fun applyStyle(isSelected: Boolean) {
            val borderColor = if (isSelected) DSSColors.primary() else DSSColors.borderDefault()
            val borderWidth = if (isSelected) 2f else 1f
            container.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = DSSColors.surface(),
                cornerRadiusDp = 12f,
                strokeColor = borderColor,
                strokeWidthDp = borderWidth,
            )
            titleLabel.setTextColor(DSSColors.textPrimary())
            lastFourLabel.setTextColor(DSSColors.textSecondary())
            phoneLabel.setTextColor(DSSColors.textSecondary())
            valueLabel.setTextColor(DSSColors.textPrimary())

            planTagLabel.setTextColor(DSSColors.textSecondary())
            planTagLabel.background = DrawableFactory.rounded(
                context = context, backgroundColor = DSSColors.divider(), cornerRadiusDp = 6f,
            )
            scheduledBadgeLabel.background = DrawableFactory.rounded(
                context = context, backgroundColor = DSSColors.primary(), cornerRadiusDp = 6f,
            )
        }

        /** Equivalente ao `PaddingLabel` do iOS (insets 4/8/4/8). */
        private fun paddingLabel(ctx: Context): TextView = TextView(ctx).apply {
            setPadding(8f.dpToPx(ctx), 4f.dpToPx(ctx), 8f.dpToPx(ctx), 4f.dpToPx(ctx))
        }
    }

    companion object {
        private val isoFormatter: SimpleDateFormat by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = true
            }
        }
        private val isoFormatterNoFraction: SimpleDateFormat by lazy {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = true
            }
        }
        private val dateDisplayFormatter: SimpleDateFormat by lazy {
            SimpleDateFormat("dd/MM/yy", Locale("pt", "BR"))
        }
        private val brlFormatter: java.text.NumberFormat by lazy {
            java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        }

        /**
         * Apresenta o bottom sheet de cartão da programada com os dados já carregados pelo app.
         * O componente NÃO faz `listCards`/`listRecurrences`.
         */
        fun present(
            activity: FragmentActivity,
            recurrence: RecurrenceModel?,
            cards: List<CardModel>,
            delegate: Delegate? = null,
            illustration: Drawable? = null,
            cardImageResolver: ((String) -> Drawable?)? = null,
            onConfirm: ((CardModel) -> Unit)? = null,
            onAddCard: (() -> Unit)? = null,
        ): DSSScheduledCardBottomSheet {
            val sheet = DSSScheduledCardBottomSheet()
            sheet.recurrence = recurrence
            sheet.cards = cards
            sheet.delegate = delegate
            sheet.illustration = illustration
            sheet.cardImageResolver = cardImageResolver
            sheet.onConfirm = onConfirm
            sheet.onAddCard = onAddCard
            sheet.hostActivity = activity
            sheet.show(activity.supportFragmentManager, "DSSScheduledCardBottomSheet")
            return sheet
        }
    }
}

/**
 * Port do `DSSScheduledCardSuccessBottomSheet.swift` — confirmação de sucesso da troca.
 */
class DSSScheduledCardSuccessBottomSheet : BottomSheetDialogFragment() {

    interface Delegate {
        fun scheduledCardSuccessBottomSheetDidFinish(sheet: DSSScheduledCardSuccessBottomSheet) {}
    }

    var delegate: Delegate? = null
    var onFinish: (() -> Unit)? = null

    /** Drawable da ilustração de sucesso, provido pelo módulo de brand. */
    var illustration: Drawable? = null

    private var didFinish = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        // iOS: containerView cornerRadius 24 com maskedCorners nos cantos superiores.
        val scroll = ScrollView(ctx).apply { background = roundedTopBackground(ctx, DSSColors.background()) }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24f.dpToPx(ctx), 12f.dpToPx(ctx), 24f.dpToPx(ctx), 40f.dpToPx(ctx))
        }

        // Handle (40x5, cantos 2.5, systemGray4) — topo 12
        val handle = View(ctx).apply {
            background = DrawableFactory.rounded(
                context = ctx, backgroundColor = DSSColors.divider(), cornerRadiusDp = 2.5f,
            )
        }
        root.addView(handle, LinearLayout.LayoutParams(
            40f.dpToPx(ctx), 5f.dpToPx(ctx),
        ).apply { gravity = Gravity.CENTER_HORIZONTAL })

        val image = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            // iOS: ImageLoader.image(named: "success_image", brand: BrandResolver.current()).
            val drawable = illustration ?: ImageLoader.image(ctx, "success_image")
            setImageDrawable(drawable)
        }
        root.addView(image, LinearLayout.LayoutParams(
            180f.dpToPx(ctx), 130f.dpToPx(ctx),
        ).apply { topMargin = 40f.dpToPx(ctx) })

        val title = TextView(ctx).apply {
            text = "Cartão da programada alterado com sucesso!"
            typeface = DSSFont.bold(ctx, 20f).typeface
            textSize = 20f
            setTextColor(DSSColors.success())
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = 24f.dpToPx(ctx) })

        scroll.addView(root, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        ))
        return scroll
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (!didFinish) {
            didFinish = true
            delegate?.scheduledCardSuccessBottomSheetDidFinish(this)
            onFinish?.invoke()
        }
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            delegate: Delegate? = null,
            illustration: Drawable? = null,
            onFinish: (() -> Unit)? = null,
        ): DSSScheduledCardSuccessBottomSheet {
            val sheet = DSSScheduledCardSuccessBottomSheet()
            sheet.delegate = delegate
            sheet.illustration = illustration
            sheet.onFinish = onFinish
            sheet.show(activity.supportFragmentManager, "DSSScheduledCardSuccessBottomSheet")
            return sheet
        }
    }
}

/**
 * Fundo com cantos superiores arredondados (24dp), espelhando o `containerView` do iOS
 * (`cornerRadius = 24` + `maskedCorners = [minXMinY, maxXMinY]`).
 */
private fun roundedTopBackground(ctx: Context, @androidx.annotation.ColorInt color: Int): Drawable {
    val r = 24f.dpToPx(ctx).toFloat()
    return android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        // iOS: borderLayer (lineWidth 1) adicionado só em black/dark.
        // black -> branco; dark -> branco @40%; light -> sem borda.
        when (ThemeManager.colorScheme) {
            ColorScheme.BLACK -> setStroke(1f.dpToPx(ctx), Color.WHITE)
            ColorScheme.DARK -> setStroke(1f.dpToPx(ctx), Color.argb(102, 255, 255, 255))
            else -> {}
        }
    }
}
