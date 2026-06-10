package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSActionCardCollectionView` do iOS — scroll horizontal de [DSSActionCardButton].
 */
class DSSActionCardCollectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    /** Item da collection — título + ação ao tocar. */
    data class Item(val title: CharSequence, val action: () -> Unit)

    private val stack = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        val pad = 16f.dpToPx(context)
        setPadding(pad, 0, pad, 0)
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    private var items: List<Item> = emptyList()

    init {
        isHorizontalScrollBarEnabled = false
        addView(stack)
    }

    fun setItems(items: List<Item>) {
        this.items = items
        reload()
    }

    private fun reload() {
        stack.removeAllViews()
        val spacing = 12f.dpToPx(context)
        val w = DSSActionCardButton.DEFAULT_WIDTH_DP.dpToPx(context)
        val h = DSSActionCardButton.DEFAULT_HEIGHT_DP.dpToPx(context)
        items.forEachIndexed { index, item ->
            val btn = DSSActionCardButton(context).apply {
                cardTitle = item.title
                setOnClickListener { item.action() }
            }
            val lp = LinearLayout.LayoutParams(w, h).apply {
                if (index > 0) leftMargin = spacing
            }
            stack.addView(btn, lp)
        }
    }

    /**
     * Espelha o `intrinsicContentSize` do iOS: altura intrínseca igual à
     * [DSSActionCardButton.DEFAULT_HEIGHT_DP] (largura sem métrica intrínseca,
     * dirigida pelo conteúdo). Quando o pai não fixa a altura (modo != EXACTLY),
     * a view assume a altura padrão do card.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (heightMode == MeasureSpec.EXACTLY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val intrinsicHeight = DSSActionCardButton.DEFAULT_HEIGHT_DP.dpToPx(context)
        val resolvedHeight = if (heightMode == MeasureSpec.AT_MOST) {
            minOf(intrinsicHeight, MeasureSpec.getSize(heightMeasureSpec))
        } else {
            intrinsicHeight
        }
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(resolvedHeight, MeasureSpec.EXACTLY),
        )
    }
}
