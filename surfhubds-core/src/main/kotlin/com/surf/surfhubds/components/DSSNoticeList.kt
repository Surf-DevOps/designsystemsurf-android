package com.surf.surfhubds.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.matchWidthParams
import com.surf.surfhubds.util.weightParams

/**
 * Título + lista com marcadores na cor `primary` (Figma do Grupo de Recarga: bloco
 * "Importante" e o "O que acontece:" dos avisos). Sem título, mostra só os itens.
 */
class DSSNoticeList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    private val titleLabel = dssText(context, DSSWeight.BOLD, 15f, DSSColors.textPrimary())
    private val bullets = mutableListOf<View>()
    private var items: List<CharSequence> = emptyList()

    init {
        orientation = VERTICAL
        setupThemeObserver()
    }

    fun configure(title: CharSequence?, items: List<CharSequence>) {
        this.items = items
        removeAllViews()
        bullets.clear()
        if (title != null) {
            titleLabel.text = title
            addView(titleLabel, matchWidthParams())
        }
        items.forEachIndexed { index, item ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.TOP
            }
            val bullet = View(context)
            bullets += bullet
            val size = 5f.dpToPx(context)
            row.addView(bullet, LayoutParams(size, size).apply {
                topMargin = 7f.dpToPx(context)
                marginEnd = 12f.dpToPx(context)
            })
            row.addView(dssText(context, DSSWeight.REGULAR, 14f, DSSColors.textSecondary()).apply {
                text = item
                setLineSpacing(3f.dpToPx(context).toFloat(), 1f)
            }, weightParams())
            val top = if (index == 0 && title == null) 0f else if (index == 0) 14f else 12f
            addView(row, matchWidthParams().apply { topMargin = top.dpToPx(context) })
        }
        refresh()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        titleLabel.setTextColor(DSSColors.textPrimary())
        bullets.forEach {
            it.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(DSSColors.primary())
            }
        }
        for (i in 0 until childCount) {
            val row = getChildAt(i) as? LinearLayout ?: continue
            (row.getChildAt(1) as? android.widget.TextView)?.setTextColor(DSSColors.textSecondary())
        }
    }
}
