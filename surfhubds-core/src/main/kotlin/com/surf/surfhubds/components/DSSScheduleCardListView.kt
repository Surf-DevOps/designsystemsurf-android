package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSScheduleCardListView.swift` (iOS) — lista de cartões cadastrados
 * com seleção e um botão "Cadastrar novo cartão" no rodapé.
 */
class DSSScheduleCardListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /**
     * Recorrência ativa de um cartão (espelha `Card.recurrence` do iOS).
     * O componente só verifica se é não-nulo (cartão "da programada").
     */
    data class Recurrence(
        val planValue: Double = 0.0,
    )

    data class CardModel(
        val id: String,
        val lastFour: String?,
        val flag: String?,
        val isDefault: Boolean,
        /** Drawable pré-resolvido da bandeira. Fallback quando [cardImageResolver] é nulo. */
        val brandIcon: android.graphics.drawable.Drawable? = null,
        /** Quando não-nulo, o cartão possui recorrência ativa (cartão "da programada"). */
        val recurrence: Recurrence? = null,
    )

    interface Delegate {
        fun onSelectCard(view: DSSScheduleCardListView, index: Int, card: CardModel)
        fun onRegisterNewCard(view: DSSScheduleCardListView)
    }

    var delegate: Delegate? = null

    /** Quando true, exibe apenas o cartão default e esconde o botão "Cadastrar novo cartão". */
    var showIsOnlyDefault: Boolean = false
        set(value) { field = value; reloadData() }

    /** Quando true, exibe apenas o(s) cartão(ões) com recorrência ativa (recurrence != null). */
    var showOnlyRecurrenceCard: Boolean = false
        set(value) { field = value; reloadData() }

    /**
     * Resolver da bandeira do cartão a partir do nome do recurso (ilvisa / ilelo / ilmaster),
     * espelhando o `ImageLoader.image(named:brand:)` do iOS. Quando nulo, usa
     * [CardModel.brandIcon]; se também nulo, faz lookup via [com.surf.surfhubds.util.ImageLoader].
     */
    var cardImageResolver: ((String) -> android.graphics.drawable.Drawable?)? = null

    private val stack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val registerButton = TextView(context).apply {
        text = "Cadastrar novo cartão"
        textSize = 14f
        typeface = DSSFont.medium(context, 14f).typeface
        isClickable = true; isFocusable = true
        gravity = Gravity.END
    }

    private var cards: List<CardModel> = emptyList()
    private var selectedIndex: Int? = null

    init {
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        column.addView(stack, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        column.addView(
            registerButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = 16f.dpToPx(context) },
        )
        // MATCH_PARENT obrigatório: sem isso o FrameLayout adiciona o column como WRAP_CONTENT,
        // e toda a cadeia (column→stack→row→container→labelsStack) colapsa pra largura intrínseca
        // (~181px) na 1ª passada de medição. O titleLabel single-line mede travado nessa largura
        // e corta "Cartão cadastrado" → "Cartão ca", mesmo o card depois esticando pra largura cheia.
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        registerButton.setOnClickListener { delegate?.onRegisterNewCard(this) }
        refresh()
        setupThemeObserver()
    }

    fun setCards(cards: List<CardModel>) {
        this.cards = cards
        reloadData()
    }

    fun getSelectedCard(): CardModel? {
        val idx = selectedIndex ?: return null
        val displayed = displayedCards()
        return displayed.getOrNull(idx)
    }

    private fun displayedCards(): List<CardModel> {
        if (showOnlyRecurrenceCard) return cards.filter { it.recurrence != null }
        if (showIsOnlyDefault) return cards.filter { it.isDefault }
        return cards
    }

    private fun reloadData() {
        stack.removeAllViews()
        val displayed = displayedCards()
        // showOnlyRecurrenceCard: trava seleção no cartão da recorrência
        // showIsOnlyDefault: força seleção no default sempre
        // caso contrário: seleciona o default apenas se não há seleção
        if (showOnlyRecurrenceCard) {
            val ri = displayed.indexOfFirst { it.recurrence != null }
            selectedIndex = when {
                ri >= 0 -> ri
                displayed.isEmpty() -> null
                else -> 0
            }
        } else if (showIsOnlyDefault) {
            selectedIndex = displayed.indexOfFirst { it.isDefault }.takeIf { it >= 0 } ?: 0
        } else if (selectedIndex == null) {
            val di = displayed.indexOfFirst { it.isDefault }
            if (di >= 0) selectedIndex = di
        }
        val locked = showIsOnlyDefault || showOnlyRecurrenceCard
        for ((i, card) in displayed.withIndex()) {
            val row = CardRowView(context)
            row.configure(card, isSelected = selectedIndex == i, image = resolveCardImage(card))
            row.setOnClickListener { if (!locked) onRowTapped(i) }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = if (i == 0) 0 else 12f.dpToPx(context) }
            stack.addView(row, lp)
        }
        registerButton.visibility = if (locked) View.GONE else View.VISIBLE
    }

    /**
     * Resolve a bandeira a partir de `card.flag` (visa/elo/master), espelhando o
     * `CardType(flag:)` + `ImageLoader.image(named:)` do iOS. Usa [cardImageResolver]
     * quando definido; senão [CardModel.brandIcon]; senão lookup via [com.surf.surfhubds.util.ImageLoader].
     */
    private fun resolveCardImage(card: CardModel): android.graphics.drawable.Drawable? {
        val flag = (card.flag ?: "").lowercase()
        val name = when {
            flag.contains("visa") -> "ilvisa"
            flag.contains("elo") -> "ilelo"
            else -> "ilmaster"
        }
        cardImageResolver?.invoke(name)?.let { return it }
        card.brandIcon?.let { return it }
        return com.surf.surfhubds.util.ImageLoader.image(context, name)
    }

    private fun onRowTapped(index: Int) {
        // Quando showIsOnlyDefault/showOnlyRecurrenceCard, seleção fica travada.
        if (showIsOnlyDefault || showOnlyRecurrenceCard) return
        val displayed = displayedCards()
        if (index >= displayed.size) return
        selectedIndex = index
        for ((i, child) in stack.children().withIndex()) {
            (child as? CardRowView)?.applyStyle(displayed[i].isDefault, i == index)
        }
        delegate?.onSelectCard(this, index, displayed[index])
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        registerButton.setTextColor(DSSColors.primary())
        for ((i, child) in stack.children().withIndex()) {
            val card = displayedCards().getOrNull(i) ?: continue
            (child as? CardRowView)?.applyStyle(card.isDefault, selectedIndex == i)
        }
    }

    private fun LinearLayout.children(): List<View> = (0 until childCount).map { getChildAt(it) }

    /** Linha visual do cartão. */
    private class CardRowView(context: Context) : FrameLayout(context), ThemeAware {

        // RelativeLayout espelhando as constraints ABSOLUTAS do iOS (sem weight!).
        // Causa raiz das tentativas anteriores: todas usavam LinearLayout horizontal com
        // `weight` no labelsStack. Na medição multi-passo do LinearLayout a redistribuição
        // por peso ENCOLHE o filho com peso abaixo da largura natural do texto, cortando
        // "Cartão cadastrado" → "Cartão ca". Sem peso (dot ancorado na leading, imagem após
        // o dot, labelsStack ancorado na trailing) o labelsStack é medido na largura natural
        // e nunca encolhe — o título não trunca em nenhuma condição de medição do host.
        private val container = RelativeLayout(context)
        private val defaultDot = View(context)
        private val cardImage = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            // iOS: cardImageView.layer.cornerRadius = 6 + clipsToBounds
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, 6f.dpToPx(context).toFloat())
                }
            }
        }
        private val labelsStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            // iOS: as duas linhas compartilham a borda esquerda (texto alinhado à esquerda
            // dentro do bloco; o bloco é que fica encostado na margem trailing).
            gravity = Gravity.START
        }
        private val titleLabel = TextView(context).apply {
            text = "Cartão cadastrado"
            textSize = 14f
            typeface = DSSFont.medium(context, 14f).typeface
            // Uma linha só, como no iOS. Um TextView single-line NÃO quebra no meio da
            // palavra; combinado com o RelativeLayout sem weight (bloco medido na largura
            // natural, ~960px disponíveis na linha) o título cabe inteiro sem cortar nem
            // virar "Cartão ca / dastrado".
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        private val lastFourLabel = TextView(context).apply {
            textSize = 14f
            typeface = DSSFont.regular(context, 14f).typeface
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        private var isDefaultCard = false
        private var isSelected = false

        init {
            // iOS (constraints absolutas no containerView, altura FIXA 80):
            //  dot.leading = container.leading + 12; image.leading = dot.trailing + 10 (=> 32);
            //  labelsStack.trailing = container.trailing - 36; tudo centerY.
            // Mapeamento 1:1 em RelativeLayout (sem weight) — ver comentário em `container`.
            val dotId = View.generateViewId()
            defaultDot.id = dotId

            // dot: leading = 12, centerY
            container.addView(
                defaultDot,
                RelativeLayout.LayoutParams(10f.dpToPx(context), 10f.dpToPx(context)).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    marginStart = 12f.dpToPx(context)
                },
            )
            // image: leading = dot.trailing + 10, centerY, 70x45
            container.addView(
                cardImage,
                RelativeLayout.LayoutParams(70f.dpToPx(context), 45f.dpToPx(context)).apply {
                    addRule(RelativeLayout.END_OF, dotId)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    marginStart = 10f.dpToPx(context)
                },
            )
            labelsStack.addView(titleLabel)
            labelsStack.addView(
                lastFourLabel,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = 2f.dpToPx(context) },
            )
            // labelsStack: trailing = container.trailing - 36, centerY, WRAP (largura natural
            // do texto → "Cartão cadastrado" nunca trunca).
            container.addView(
                labelsStack,
                RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    marginEnd = 36f.dpToPx(context)
                },
            )
            // iOS: containerView.heightAnchor == 80 (altura fixa, não mínima).
            addView(container, LayoutParams(LayoutParams.MATCH_PARENT, 80f.dpToPx(context)))
            refresh()
            setupThemeObserver()
        }

        fun configure(card: CardModel, isSelected: Boolean, image: android.graphics.drawable.Drawable?) {
            this.isDefaultCard = card.isDefault
            lastFourLabel.text = "Final ${card.lastFour ?: "****"}"
            cardImage.setImageDrawable(image)
            applyStyle(card.isDefault, isSelected)
        }

        fun applyStyle(isDefault: Boolean, isSelected: Boolean) {
            this.isDefaultCard = isDefault
            this.isSelected = isSelected
            refresh()
        }

        override fun applyTheme(theme: Theme) { refresh() }

        private fun refresh() {
            val borderColor = if (isSelected) DSSColors.success() else DSSColors.borderDefault()
            val borderWidth = if (isSelected) 2f else 1f
            container.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = DSSColors.surface(),
                cornerRadiusDp = 12f,
                strokeColor = borderColor,
                strokeWidthDp = borderWidth,
            )
            if (isSelected) {
                defaultDot.visibility = View.VISIBLE
                defaultDot.background = DrawableFactory.rounded(
                    context = context,
                    backgroundColor = DSSColors.success(),
                    cornerRadiusDp = 5f,
                )
                titleLabel.typeface = DSSFont.medium(context, 14f).typeface
            } else {
                // iOS: `defaultDotView.isHidden = true` (sem colapsar). INVISIBLE preserva os
                // 10dp reservados, então a imagem do cartão não desloca ao (de)selecionar.
                defaultDot.visibility = View.INVISIBLE
                titleLabel.typeface = DSSFont.light(context, 14f).typeface
            }
            titleLabel.setTextColor(DSSColors.textPrimary())
            lastFourLabel.setTextColor(DSSColors.textSecondary())
        }
    }
}
