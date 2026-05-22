package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
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

    /** Modelo de cartão. */
    data class Card(
        val flag: String?,
        val lastFour: String?,
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
        this.cards.clear()
        this.cards.addAll(cards)
        adapter.notifyDataSetChanged()
    }

    fun getSelectedCard(): Card? = selectedIndex?.let { cards.getOrNull(it) }

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
            val lp = holder.itemView.layoutParams as RecyclerView.LayoutParams
            lp.width = cardWidthDp.dpToPx(context)
            lp.height = cardHeightDp.dpToPx(context)
            if (scrollDirection == RecyclerView.HORIZONTAL) {
                lp.rightMargin = 16f.dpToPx(context)
            } else {
                lp.bottomMargin = 16f.dpToPx(context)
            }
            holder.itemView.layoutParams = lp

            val selected = (showSelectionBorder && position == selectedIndex)
            holder.cell.setSelectionBorder(
                show = selected,
                colorInt = selectionBorderColor,
                widthDp = selectionBorderWidthDp,
            )
            holder.itemView.setOnClickListener {
                delegate?.didSelectCard(this@DSSCardCollectionView, position)
            }
        }
    }

    private class CardViewHolder(val cell: CardCellView) : RecyclerView.ViewHolder(cell)

    /** Célula que mostra a bandeira + "Final XXXX". */
    private class CardCellView(context: Context) : FrameLayout(context) {
        private val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        private val lastDigitsLabel = TextView(context).apply {
            textSize = 12f
            typeface = DSSFont.medium(context, 12f).typeface
            setTextColor(Color.WHITE)
        }

        init {
            addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            val lblLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            addView(lastDigitsLabel, lblLp)
            clipToOutline = true
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
            lp.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
            lp.leftMargin = leadDp.dpToPx(context)
            lp.bottomMargin = bottomDp.dpToPx(context)
            lastDigitsLabel.layoutParams = lp
        }

        fun setSelectionBorder(show: Boolean, colorInt: Int, widthDp: Float) {
            background = if (show) {
                DrawableFactory.rounded(
                    context = context,
                    backgroundColor = Color.TRANSPARENT,
                    cornerRadiusDp = 8f,
                    strokeColor = colorInt,
                    strokeWidthDp = widthDp,
                )
            } else null
        }
    }
}
