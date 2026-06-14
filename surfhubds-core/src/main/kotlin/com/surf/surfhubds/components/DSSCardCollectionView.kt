package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import kotlin.math.roundToInt

/**
 * Port do `DSSCardCollectionView` do iOS — collection horizontal/vertical
 * exibindo cartões com bandeira (Visa/Master/Elo) e últimos 4 dígitos.
 */
class DSSCardCollectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Tipos de bandeira suportados. */
    enum class CardType { VISA, MASTER, ELO }

    /**
     * Recorrência ativa de um cartão (espelha `Card.recurrence` do iOS).
     * `planValue` vem em CENTAVOS, como no iOS.
     */
    data class Recurrence(
        val planValue: Double,
    )

    /** Modelo de cartão. */
    data class Card(
        val flag: String?,
        val lastFour: String?,
        /** Quando não-nulo, o cartão é "da programada" (possui recorrência ativa). */
        val recurrence: Recurrence? = null,
    )

    interface Delegate {
        fun didSelectCard(view: DSSCardCollectionView, index: Int)
        fun didLongPressCard(view: DSSCardCollectionView, index: Int)
    }

    var delegate: Delegate? = null

    /** LinearLayoutManager.HORIZONTAL ou VERTICAL. */
    var scrollDirection: Int = RecyclerView.HORIZONTAL
        set(value) {
            field = value
            (recyclerView.layoutManager as? LinearLayoutManager)?.orientation = value
        }

    var cardWidthDp: Float = 255f
        set(value) { field = value; adapter.notifyDataSetChanged() }
    var cardHeightDp: Float = 160f
        set(value) { field = value; adapter.notifyDataSetChanged() }

    var showSelectionBorder: Boolean = true
    var selectionBorderColor: Int = Color.RED
    var selectionBorderWidthDp: Float = 2f

    /** Override para resolver os drawables por bandeira; por padrão retorna null. */
    var cardImageResolver: (CardType) -> android.graphics.drawable.Drawable? = { null }

    private val cards = mutableListOf<Card>()
    private var selectedIndex: Int? = null

    /** Verdadeiro quando ao menos um cartão possui recorrência ativa. */
    private val hasAnyProgramada: Boolean
        get() = cards.any { it.recurrence != null }

    private val recyclerView: RecyclerView = RecyclerView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        clipToPadding = false
        val pad = 16f.dpToPx(context)
        setPadding(pad, 0, pad, 0)
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }

    private val adapter = CardAdapter()

    init {
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        recyclerView.adapter = adapter
        attachLongPress()
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackgroundColor(DSSColors.surface())
    }

    fun setCards(cards: List<Card>) {
        // Cartão(ões) com recorrência sempre primeiro, mantendo a ordem original dos demais.
        this.cards.clear()
        this.cards.addAll(cards.filter { it.recurrence != null } + cards.filter { it.recurrence == null })
        adapter.notifyDataSetChanged()
    }

    fun getSelectedCard(): Card? = selectedIndex?.let { cards.getOrNull(it) }

    /**
     * Texto da faixa de programada quando o cartão tem recorrência ativa.
     * `planValue` vem em centavos, então é convertido para reais.
     */
    private fun programadaText(card: Card): String? {
        val recurrence = card.recurrence ?: return null
        val reais = recurrence.planValue / 100.0
        val valueStr = if (reais == reais.roundToInt().toDouble()) {
            "R$${reais.toInt()}"
        } else {
            String.format(java.util.Locale.US, "R$%.2f", reais).replace(".", ",")
        }
        return "Cartão com programada de $valueStr"
    }

    fun clearSelection() {
        selectedIndex = null
        adapter.notifyDataSetChanged()
    }

    private fun attachLongPress() {
        val gd = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                val child = recyclerView.findChildViewUnder(e.x, e.y) ?: return
                val pos = recyclerView.getChildAdapterPosition(child)
                if (pos == RecyclerView.NO_POSITION) return
                selectedIndex = pos
                adapter.notifyDataSetChanged()
                delegate?.didLongPressCard(this@DSSCardCollectionView, pos)
            }
        })
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gd.onTouchEvent(e); return false
            }
        })
    }

    private fun resolveCardType(flag: String?): CardType {
        val raw = flag?.lowercase() ?: return CardType.MASTER
        return when {
            "visa" in raw -> CardType.VISA
            "elo" in raw -> CardType.ELO
            else -> CardType.MASTER
        }
    }

    private inner class CardAdapter : RecyclerView.Adapter<CardViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
            val cellView = CardCellView(parent.context)
            return CardViewHolder(cellView)
        }

        override fun getItemCount(): Int = cards.size

        override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
            val card = cards[position]
            val type = resolveCardType(card.flag)
            holder.cell.configure(
                image = cardImageResolver(type),
                finalDigits = card.lastFour ?: "",
                type = type,
            )
            // Faixa "Cartão com programada de R$XX". Quando há programada, TODAS as células
            // reservam o espaço do topo para manter os cards alinhados.
            holder.cell.setProgramada(programadaText(card), reserveTopSpace = hasAnyProgramada)

            val lp = (holder.itemView.layoutParams as? RecyclerView.LayoutParams)
                ?: RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
            lp.width = cardWidthDp.dpToPx(context)
            // Card mantém o tamanho original; quando há programada, reserva a altura extra da faixa.
            val extra = if (hasAnyProgramada) CardCellView.PROGRAMADA_REVEAL_DP else 0f
            lp.height = (cardHeightDp + extra).dpToPx(context)
            if (scrollDirection == RecyclerView.HORIZONTAL) {
                lp.rightMargin = 16f.dpToPx(context)
                lp.bottomMargin = 0
            } else {
                lp.bottomMargin = 16f.dpToPx(context)
                lp.rightMargin = 0
            }
            holder.itemView.layoutParams = lp

            // showSelectionBorder controla apenas a exibição da borda (como no iOS),
            // não o comportamento de seleção/toggle.
            holder.cell.setSelectionBorder(
                borderEnabled = showSelectionBorder,
                isSelected = position == selectedIndex,
                colorInt = selectionBorderColor,
                widthDp = selectionBorderWidthDp,
            )
            holder.itemView.setOnClickListener { handleTap(position) }
        }
    }

    /**
     * Toque no card: toca no já selecionado -> deseleciona (sem disparar callback).
     * Toca em outro -> seleciona e dispara [Delegate.didSelectCard].
     * Espelha o `didSelectItemAt` do iOS.
     */
    private fun handleTap(position: Int) {
        if (selectedIndex == position) {
            selectedIndex = null
            adapter.notifyDataSetChanged()
            return
        }
        selectedIndex = position
        adapter.notifyDataSetChanged()
        delegate?.didSelectCard(this@DSSCardCollectionView, position)
    }

    private class CardViewHolder(val cell: CardCellView) : RecyclerView.ViewHolder(cell)

    /** Célula que mostra a faixa de programada (acima) + a bandeira + "Final XXXX". */
    private class CardCellView(context: Context) : FrameLayout(context) {

        /** Container do card propriamente dito (imagem + dígitos); recebe a borda de seleção. */
        private val cardContainer = FrameLayout(context).apply { clipToOutline = true }

        private val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        private val lastDigitsLabel = TextView(context).apply {
            textSize = 12f
            typeface = DSSFont.medium(context, 12f).typeface
            setTextColor(Color.WHITE)
        }

        // Faixa "Cartão com programada de R$XX" (fica acima do card).
        private val programadaBadge = TextView(context).apply {
            textSize = 13f
            typeface = DSSFont.medium(context, 13f).typeface
            setTextColor(DSSColors.success())
            gravity = Gravity.CENTER
            maxLines = 1
            // iOS: adjustsFontSizeToFitWidth = true, minimumScaleFactor = 0.8 (13 * 0.8 ≈ 10).
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this, 10, 13, 1, android.util.TypedValue.COMPLEX_UNIT_SP,
            )
            setPadding(
                12f.dpToPx(context), 6f.dpToPx(context),
                12f.dpToPx(context), 18f.dpToPx(context),
            )
            background = DrawableFactory.rounded(
                context = context,
                backgroundColor = DSSColors.surface(),
                cornerRadiusDp = 10f,
                strokeColor = DSSColors.borderDefault(),
                strokeWidthDp = 1f,
            )
            visibility = View.GONE
        }

        init {
            // Ordem (z): faixa atrás, card por cima (deslocado pra baixo quando há reserva).
            addView(
                programadaBadge,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.TOP
                    leftMargin = 8f.dpToPx(context)
                    rightMargin = 8f.dpToPx(context)
                },
            )
            cardContainer.addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            cardContainer.addView(lastDigitsLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            addView(cardContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        fun configure(image: android.graphics.drawable.Drawable?, finalDigits: String, type: CardType) {
            imageView.setImageDrawable(image)
            lastDigitsLabel.text = "Final $finalDigits"

            val (leadDp, bottomDp) = when (type) {
                CardType.VISA -> 65f to 15f
                CardType.ELO -> 70f to 15f
                CardType.MASTER -> 60f to 20f
            }
            val lp = lastDigitsLabel.layoutParams as LayoutParams
            lp.gravity = Gravity.BOTTOM or Gravity.START
            lp.leftMargin = leadDp.dpToPx(context)
            lp.bottomMargin = bottomDp.dpToPx(context)
            lastDigitsLabel.layoutParams = lp
        }

        /**
         * Mostra/esconde a faixa de programada acima do card.
         * @param text texto da faixa; null/vazio esconde a faixa.
         * @param reserveTopSpace quando true, reserva o espaço do topo (mantém os cards
         *   alinhados) mesmo que este card não tenha a faixa.
         */
        fun setProgramada(text: String?, reserveTopSpace: Boolean) {
            val reveal = if (reserveTopSpace) PROGRAMADA_REVEAL_DP.dpToPx(context) else 0
            (cardContainer.layoutParams as? LayoutParams)?.let {
                if (it.topMargin != reveal) {
                    it.topMargin = reveal
                    cardContainer.layoutParams = it
                }
            }
            if (!text.isNullOrEmpty()) {
                programadaBadge.text = text
                programadaBadge.visibility = View.VISIBLE
            } else {
                programadaBadge.visibility = View.GONE
            }
        }

        fun setSelectionBorder(borderEnabled: Boolean, isSelected: Boolean, colorInt: Int, widthDp: Float) {
            // iOS aplica cornerRadius 8 + clipsToBounds sempre que a borda está habilitada.
            // No Android a imageView é MATCH_PARENT e fica DESENHADA POR CIMA do background:
            // se a borda fosse só o stroke do background, a imagem full-bleed a cobriria e
            // sobraria apenas uma listra no topo. Por isso:
            //   - background = retângulo transparente arredondado → fornece o outline p/ o
            //     clipToOutline arredondar a imagem (cantos do card);
            //   - foreground = o stroke colorido → desenhado POR CIMA da imagem, então a
            //     borda fecha o card inteiro nos 4 lados.
            if (borderEnabled) {
                cardContainer.background = DrawableFactory.rounded(
                    context = context,
                    backgroundColor = Color.TRANSPARENT,
                    cornerRadiusDp = 8f,
                )
                cardContainer.foregroundGravity = Gravity.FILL
                cardContainer.foreground = if (isSelected) {
                    DrawableFactory.rounded(
                        context = context,
                        backgroundColor = Color.TRANSPARENT,
                        cornerRadiusDp = 8f,
                        strokeColor = colorInt,
                        strokeWidthDp = widthDp,
                    )
                } else {
                    null
                }
            } else {
                cardContainer.background = null
                cardContainer.foreground = null
            }
        }

        companion object {
            /** Espaço reservado acima do card para a faixa de programada (pt -> dp). */
            const val PROGRAMADA_REVEAL_DP = 24f
        }
    }
}
