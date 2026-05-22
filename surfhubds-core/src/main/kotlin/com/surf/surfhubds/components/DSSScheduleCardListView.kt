package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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

    data class CardModel(
        val id: String,
        val lastFour: String?,
        val flag: String?,
        val isDefault: Boolean,
        val brandIcon: android.graphics.drawable.Drawable? = null,
    )

    interface Delegate {
        fun onSelectCard(view: DSSScheduleCardListView, index: Int, card: CardModel)
        fun onRegisterNewCard(view: DSSScheduleCardListView)
    }

    var delegate: Delegate? = null

    /** Quando true, exibe apenas o cartão default e esconde o botão "Cadastrar novo cartão". */
    var showIsOnlyDefault: Boolean = false
        set(value) { field = value; reloadData() }

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
        addView(column)
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
        return if (showIsOnlyDefault) cards.filter { it.isDefault } else cards
    }

    private fun reloadData() {
        stack.removeAllViews()
        val displayed = displayedCards()
        if (showIsOnlyDefault) {
            selectedIndex = displayed.indexOfFirst { it.isDefault }.takeIf { it >= 0 } ?: 0
        } else if (selectedIndex == null) {
            val di = displayed.indexOfFirst { it.isDefault }
            if (di >= 0) selectedIndex = di
        }
        for ((i, card) in displayed.withIndex()) {
            val row = CardRowView(context)
            row.configure(card, isSelected = selectedIndex == i)
            row.setOnClickListener { if (!showIsOnlyDefault) onRowTapped(i) }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = if (i == 0) 0 else 12f.dpToPx(context) }
            stack.addView(row, lp)
        }
        registerButton.visibility = if (showIsOnlyDefault) View.GONE else View.VISIBLE
    }

    private fun onRowTapped(index: Int) {
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

        private val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        private val defaultDot = View(context)
        private val cardImage = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        private val labelsStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        private val titleLabel = TextView(context).apply {
            text = "Cartão cadastrado"
            textSize = 14f
            typeface = DSSFont.medium(context, 14f).typeface
        }
        private val lastFourLabel = TextView(context).apply {
            textSize = 14f
            typeface = DSSFont.regular(context, 14f).typeface
        }

        private var isDefaultCard = false
        private var isSelected = false

        init {
            val hPad = 12f.dpToPx(context); val vPad = 12f.dpToPx(context)
            container.setPadding(hPad, vPad, hPad, vPad)
            container.minimumHeight = 80f.dpToPx(context)

            container.addView(
                defaultDot,
                LinearLayout.LayoutParams(10f.dpToPx(context), 10f.dpToPx(context)),
            )
            container.addView(
                cardImage,
                LinearLayout.LayoutParams(70f.dpToPx(context), 45f.dpToPx(context)).apply {
                    leftMargin = 10f.dpToPx(context)
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
            container.addView(
                labelsStack,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = 12f.dpToPx(context); rightMargin = 36f.dpToPx(context)
                },
            )
            addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            refresh()
            setupThemeObserver()
        }

        fun configure(card: CardModel, isSelected: Boolean) {
            this.isDefaultCard = card.isDefault
            lastFourLabel.text = "Final ${card.lastFour ?: "****"}"
            cardImage.setImageDrawable(card.brandIcon)
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
                defaultDot.visibility = View.GONE
                titleLabel.typeface = DSSFont.light(context, 14f).typeface
            }
            titleLabel.setTextColor(DSSColors.textPrimary())
            lastFourLabel.setTextColor(DSSColors.textSecondary())
        }
    }
}
