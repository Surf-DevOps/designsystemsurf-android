package com.surf.surfhubds.components

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSExpandableListView` do iOS — lista vertical de [DSSExpandableItemView],
 * cada item é uma pergunta/resposta de FAQ.
 *
 * Use [configure] (lista de `DSSFAQItem`) ou [updateItems] (lista de pares).
 */
class DSSExpandableListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
    }

    fun configure(items: List<DSSFAQItem>) {
        rebuild(items)
    }

    /**
     * Compat: aceita pares (question, answer).
     */
    fun updateItems(items: List<Pair<String, String>>) {
        rebuild(items.map { DSSFAQItem(it.first, it.second) })
    }

    private fun rebuild(items: List<DSSFAQItem>) {
        removeAllViews()
        items.forEachIndexed { index, item ->
            val card = DSSExpandableItemView(context).apply {
                configure(item.question, item.answer)
            }
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                if (index > 0) topMargin = 8f.dpToPx(context)
            }
            addView(card, lp)
        }
    }
}
