package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSSelectionButtonView` do iOS — grid 2 colunas de [DSSSelectionButton]
 * para selecionar categoria de FAQ.
 *
 * Quando há 0 ou 1 categoria, a view não renderiza nada (vazia).
 *
 * As classes [DSSFAQCategory] e [DSSFAQItem] vivem em [FAQLoader].
 */
class DSSSelectionButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val buttons = mutableListOf<DSSSelectionButton>()
    private var orderedCategories: List<DSSFAQCategory> = emptyList()

    var selectedCategory: String? = null
        private set

    var didSelectCategory: ((List<DSSFAQItem>) -> Unit)? = null

    @ColorInt private var selectTitleColor: Int = Color.WHITE
    @ColorInt private var unselectTitleColor: Int = Color.DKGRAY
    @ColorInt private var borderColorOverride: Int = DSSColors.textPrimary()

    init {
        orientation = VERTICAL
    }

    /**
     * Configura com categorias ordenadas.
     */
    fun configure(
        categories: List<DSSFAQCategory>,
        @ColorInt selectTitleColor: Int = Color.WHITE,
        @ColorInt unselectTitleColor: Int = Color.DKGRAY,
        @ColorInt borderColor: Int = DSSColors.textPrimary(),
    ) {
        this.orderedCategories = categories
        this.selectTitleColor = selectTitleColor
        this.unselectTitleColor = unselectTitleColor
        this.borderColorOverride = borderColor
        rebuild()
    }

    /**
     * Backwards-compat: aceita um Map<String, List<DSSFAQItem>>. A ordem
     * pode variar pois os dicts não são ordenados.
     */
    fun configure(
        categories: Map<String, List<DSSFAQItem>>,
        @ColorInt selectTitleColor: Int = Color.WHITE,
        @ColorInt unselectTitleColor: Int = Color.DKGRAY,
        @ColorInt borderColor: Int = DSSColors.textPrimary(),
    ) {
        val mapped = categories.map { (title, items) -> DSSFAQCategory(title, items) }
        configure(mapped, selectTitleColor, unselectTitleColor, borderColor)
    }

    private fun rebuild() {
        removeAllViews()
        buttons.clear()

        val count = orderedCategories.size
        if (count <= 1) return

        val columns = 2
        val rows = ((count + columns - 1) / columns)

        for (row in 0 until rows) {
            val rowStack = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val rowLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = if (row > 0) 8f.dpToPx(context) else 0
            }
            addView(rowStack, rowLp)

            for (col in 0 until columns) {
                val index = row * columns + col
                if (index < orderedCategories.size) {
                    val category = orderedCategories[index]
                    val button = DSSSelectionButton(context).apply {
                        configure(
                            title = category.title,
                            selectedTitleColor = selectTitleColor,
                            unselectedTitleColor = unselectTitleColor,
                            borderColor = borderColorOverride,
                        )
                        tag = index
                        setOnClickListener { handleSelection(this) }
                    }
                    buttons.add(button)
                    val buttonLp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (col > 0) leftMargin = 8f.dpToPx(context)
                    }
                    rowStack.addView(button, buttonLp)

                    if (index == 0) {
                        button.isSelected = true
                        selectedCategory = category.title
                        Handler(Looper.getMainLooper()).post {
                            didSelectCategory?.invoke(itemsForCategoryAt(0))
                        }
                    }
                } else {
                    // placeholder pra manter o grid alinhado
                    val placeholder = View(context)
                    val placeLp = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (col > 0) leftMargin = 8f.dpToPx(context)
                    }
                    rowStack.addView(placeholder, placeLp)
                }
            }
        }
    }

    private fun handleSelection(sender: DSSSelectionButton) {
        for (button in buttons) {
            button.isSelected = (button === sender)
        }
        val index = sender.tag as? Int ?: return
        if (index >= orderedCategories.size) return
        selectedCategory = orderedCategories[index].title
        didSelectCategory?.invoke(itemsForCategoryAt(index))
    }

    private fun itemsForCategoryAt(index: Int): List<DSSFAQItem> {
        if (index < 0 || index >= orderedCategories.size) return emptyList()
        return orderedCategories[index].items
    }
}
