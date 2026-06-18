package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.AppStrings
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.ImageLoader
import com.surf.surfhubds.util.dpToPx

/**
 * Modelo de cartão de crédito usado pela [DSSPaymentSelectionView] — equivalente neutro
 * ao `Card` do `SurfAPIKit` no iOS.
 */
data class DSSPaymentCard(
    val id: String,
    val lastFour: String,
    val flag: String,
    /** Drawable do ícone da bandeira, resolvido pela app. */
    val brandIcon: Drawable? = null,
)

/**
 * Tipos de método de pagamento. Use [DSSPaymentMethod.CreditCard] para cartões salvos.
 */
sealed class DSSPaymentMethod {
    object Pix : DSSPaymentMethod()
    object NewCard : DSSPaymentMethod()
    data class CreditCard(val index: Int) : DSSPaymentMethod()
}

/**
 * Delegate do `DSSPaymentSelectionView`.
 */
interface DSSPaymentSelectionViewDelegate {
    fun didSelectPix(view: DSSPaymentSelectionView, card: DSSPaymentMethodCard) = Unit
    fun didSelectNewCard(view: DSSPaymentSelectionView, card: DSSPaymentMethodCard) = Unit
    fun didSelectCreditCard(
        view: DSSPaymentSelectionView,
        card: DSSPaymentMethodCard,
        cardData: DSSPaymentCard,
        index: Int,
    ) = Unit

    fun didDeselectPix(view: DSSPaymentSelectionView, card: DSSPaymentMethodCard) = Unit
    fun didDeselectCreditCard(
        view: DSSPaymentSelectionView,
        card: DSSPaymentMethodCard,
        cardData: DSSPaymentCard,
        index: Int,
    ) = Unit
}

/**
 * Port do `DSSPaymentSelectionView` do iOS — view com cards fixos de Pix e Novo Cartão,
 * mais uma lista de cartões salvos. Apenas um método pode ficar selecionado por vez.
 */
class DSSPaymentSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    var delegate: DSSPaymentSelectionViewDelegate? = null

    private val rootStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val fixedStack = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    /** Ícone do Pix — opcionalmente injetado pela app. */
    var pixIcon: Drawable? = null
        set(value) { field = value; pixCard.setIcon(value) }

    /** Ícone "novo cartão" — opcionalmente injetado pela app. */
    var newCardIcon: Drawable? = null
        set(value) { field = value; newCardCard.setIcon(value) }

    private val pixCard = DSSPaymentMethodCard(context).apply {
        configure(PaymentMethodImages.pixIcon(context), AppStrings.brand(context, "card_plan_pix", "Pix"))
    }
    private val newCardCard = DSSPaymentMethodCard(context).apply {
        configure(PaymentMethodImages.addCardIcon(context), AppStrings.brand(context, "payment_method_new_credit_card", "Novo Cartão de Crédito"))
    }

    private val creditCardsRecycler = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        isNestedScrollingEnabled = true
        // iOS: showsVerticalScrollIndicator = true; bounces / alwaysBounceVertical = true.
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_ALWAYS
    }

    private val adapter = CreditCardAdapter()

    private var creditCards: List<DSSPaymentCard> = emptyList()
    private var selected: DSSPaymentMethod? = null

    init {
        addView(rootStack, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val hPad = 16f.dpToPx(context)
        val vPad = 20f.dpToPx(context)
        rootStack.setPadding(0, vPad, 0, 0)

        // Fixed section (Pix + new card)
        fixedStack.setPadding(hPad, 0, hPad, 0)
        val itemSpacing = 12f.dpToPx(context)
        fixedStack.addView(pixCard, fixedItemLp(itemSpacing, first = true))
        fixedStack.addView(newCardCard, fixedItemLp(itemSpacing, first = false))
        rootStack.addView(
            fixedStack,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // Credit cards section
        creditCardsRecycler.adapter = adapter
        creditCardsRecycler.setPadding(hPad, 0, hPad, 0)
        creditCardsRecycler.clipToPadding = false
        rootStack.addView(
            creditCardsRecycler,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
            ).apply { topMargin = 20f.dpToPx(context) },
        )

        pixCard.setOnClickListener { onPixTapped() }
        newCardCard.setOnClickListener { onNewCardTapped() }

        updateRecyclerHeight()
        refresh()
        setupThemeObserver()
    }

    /** Substitui a lista de cartões salvos. */
    fun setCreditCards(cards: List<DSSPaymentCard>) {
        creditCards = cards
        adapter.submit(cards, selected)
        updateRecyclerHeight()
    }

    /** Acrescenta um cartão. */
    fun addCreditCard(card: DSSPaymentCard) {
        creditCards = creditCards + card
        adapter.submit(creditCards, selected)
        updateRecyclerHeight()
    }

    /** Remove o cartão no índice indicado. */
    fun removeCreditCard(index: Int) {
        if (index !in creditCards.indices) return
        creditCards = creditCards.toMutableList().also { it.removeAt(index) }
        adapter.submit(creditCards, selected)
        updateRecyclerHeight()
    }

    fun setSelectedPaymentMethod(method: DSSPaymentMethod?) {
        selected = method
        updateSelectedState()
    }

    fun getSelectedPaymentMethod(): DSSPaymentMethod? = selected

    fun getSelectedCreditCardData(): DSSPaymentCard? {
        val s = selected
        return if (s is DSSPaymentMethod.CreditCard && s.index in creditCards.indices) {
            creditCards[s.index]
        } else null
    }

    fun setPixVisibility(visible: Boolean) {
        pixCard.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible && selected == DSSPaymentMethod.Pix) {
            selected = null
            updateSelectedState()
        }
    }

    fun setNewCardVisibility(visible: Boolean) {
        newCardCard.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        // Espelha applyColors() do iOS: scheme black → .black; dark →
        // .secondarySystemBackground (#1C1C1E); light → DSSColors.backgroundSecondary.
        val bg = when {
            ThemeManager.colorScheme == ColorScheme.BLACK -> Color.BLACK
            ThemeManager.colorScheme == ColorScheme.DARK -> SECONDARY_SYSTEM_BACKGROUND_DARK
            else -> DSSColors.backgroundSecondary()
        }
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = bg,
            cornerRadiusDp = 12f,
        )
    }

    private fun onPixTapped() {
        if (selected == DSSPaymentMethod.Pix) {
            selected = null
            updateSelectedState()
            delegate?.didDeselectPix(this, pixCard)
        } else {
            selected = DSSPaymentMethod.Pix
            updateSelectedState()
            delegate?.didSelectPix(this, pixCard)
        }
    }

    private fun onNewCardTapped() {
        delegate?.didSelectNewCard(this, newCardCard)
    }

    private fun updateSelectedState() {
        pixCard.setSelectedState(selected == DSSPaymentMethod.Pix)
        newCardCard.setSelectedState(selected == DSSPaymentMethod.NewCard)
        adapter.updateSelection(selected)
    }

    private fun updateRecyclerHeight() {
        val cardHeight = 56f.dpToPx(context)
        val spacing = 12f.dpToPx(context)
        val n = creditCards.size
        val maxHeight = 300f.dpToPx(context)
        val target = if (n == 0) 0 else {
            (n * cardHeight + (n - 1) * spacing).coerceAtMost(maxHeight)
        }
        creditCardsRecycler.visibility = if (n == 0) View.GONE else View.VISIBLE
        val lp = creditCardsRecycler.layoutParams
        lp.height = target
        creditCardsRecycler.layoutParams = lp
    }

    private fun fixedItemLp(spacingPx: Int, first: Boolean): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = if (first) 0 else spacingPx }

    /**
     * Espelha `getCardIcon(for:)` + `CardType(rawValue:)` do iOS: só "VISA", "ELO" e
     * "MASTERCARD" (uppercased) resolvem para os assets de brand `ilVisa`/`ilElo`/
     * `ilMaster`; qualquer outra bandeira fica sem ícone (`nil` no iOS).
     */
    private fun cardIconForFlag(context: Context, flag: String): Drawable? =
        when (flag.uppercase()) {
            "VISA" -> ImageLoader.image(context, "ilVisa")
            "ELO" -> ImageLoader.image(context, "ilElo")
            "MASTERCARD" -> ImageLoader.image(context, "ilMaster")
            else -> null
        }

    // MARK: Adapter

    private inner class CreditCardAdapter :
        RecyclerView.Adapter<CreditCardAdapter.VH>() {

        private val items = mutableListOf<DSSPaymentCard>()
        private var selectedMethod: DSSPaymentMethod? = null

        fun submit(list: List<DSSPaymentCard>, sel: DSSPaymentMethod?) {
            items.clear()
            items.addAll(list)
            selectedMethod = sel
            notifyDataSetChanged()
        }

        fun updateSelection(sel: DSSPaymentMethod?) {
            selectedMethod = sel
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val card = DSSPaymentMethodCard(parent.context)
            card.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT,
            )
            return VH(card)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val card = items[position]
            // iOS: minimumLineSpacing = 12 entre células; a primeira não tem espaçamento
            // no topo (sectionInset.top = 0). Aplica 12dp apenas a partir do 2º item.
            (holder.card.layoutParams as? RecyclerView.LayoutParams)?.let { lp ->
                lp.topMargin = if (position == 0) 0 else 12f.dpToPx(holder.card.context)
                holder.card.layoutParams = lp
            }
            // Espelha a célula do iOS: título via brand string
            // ("card_collection.final_digits_format") e ícone da bandeira resolvido
            // internamente via ImageLoader quando a app não injetar um.
            val title = AppStrings.brand(
                holder.card.context,
                "card_collection_final_digits_format",
                "Final %s",
                card.lastFour,
            )
            val icon = card.brandIcon ?: cardIconForFlag(holder.card.context, card.flag)
            holder.card.configure(icon, title)
            val selected = (selectedMethod as? DSSPaymentMethod.CreditCard)?.index == position
            holder.card.setSelectedState(selected)
            holder.card.setOnClickListener {
                val current = (selectedMethod as? DSSPaymentMethod.CreditCard)?.index
                if (current == position) {
                    this@DSSPaymentSelectionView.selected = null
                    this@DSSPaymentSelectionView.updateSelectedState()
                    delegate?.didDeselectCreditCard(
                        this@DSSPaymentSelectionView, holder.card, card, position,
                    )
                } else {
                    this@DSSPaymentSelectionView.selected = DSSPaymentMethod.CreditCard(position)
                    this@DSSPaymentSelectionView.updateSelectedState()
                    delegate?.didSelectCreditCard(
                        this@DSSPaymentSelectionView, holder.card, card, position,
                    )
                }
            }
        }

        override fun getItemCount(): Int = items.size

        inner class VH(val card: DSSPaymentMethodCard) : RecyclerView.ViewHolder(card)
    }

    companion object {
        /** iOS: UIColor.secondarySystemBackground no dark (#1C1C1E). */
        private const val SECONDARY_SYSTEM_BACKGROUND_DARK = 0xFF1C1C1E.toInt()
    }
}
