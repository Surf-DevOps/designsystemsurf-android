package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DSSWeight
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dssText
import com.surf.surfhubds.util.weightParams
import com.surf.surfhubds.util.wrapParams

/**
 * Lista de opções com seleção única (Figma do Grupo de Recarga: planos "Até 20GB · Validade
 * 30 dias · R$ 30,00 por linha" e o "Alterar plano de Ana"). A opção marcada fica com borda
 * forte e fundo levemente tingido; [showsRadio] mostra o rádio à esquerda. Uma etiqueta
 * opcional ("Plano atual") fica presa na borda de cima, à direita.
 *
 * [onSelectionChanged] só dispara por toque.
 */
class DSSChoiceList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    data class Item(
        val id: String,
        val title: String,
        /** Texto pequeno acima do título ("Até"). */
        val overline: String? = null,
        /** Texto pequeno abaixo do título ("Validade 30 dias", "11 9 2222-1010"). */
        val subtitle: String? = null,
        val value: String? = null,
        /** Texto pequeno abaixo do valor ("por linha"). */
        val valueCaption: String? = null,
        /** Etiqueta na borda ("Plano atual"). */
        val tag: String? = null,
    )

    var onSelectionChanged: ((Item) -> Unit)? = null

    var showsRadio: Boolean = true
        set(value) { field = value; rows.forEach { it.showsRadio = value } }

    var selectedId: String? = null
        private set

    private val rows = mutableListOf<Row>()
    private var items: List<Item> = emptyList()

    init {
        orientation = VERTICAL
        setupThemeObserver()
    }

    fun configure(items: List<Item>, selectedId: String?) {
        this.items = items
        this.selectedId = selectedId
        removeAllViews()
        rows.clear()
        items.forEachIndexed { index, item ->
            val row = Row(context, item).apply {
                showsRadio = this@DSSChoiceList.showsRadio
                setOnClickListener { select(item.id, fromUser = true) }
            }
            rows += row
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                if (index > 0) topMargin = 10f.dpToPx(context)
            })
        }
        refresh()
    }

    fun select(id: String?, fromUser: Boolean = false) {
        selectedId = id
        refresh()
        if (fromUser) items.firstOrNull { it.id == id }?.let { onSelectionChanged?.invoke(it) }
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        rows.forEach { it.setChecked(it.item.id == selectedId) }
    }

    /** Uma opção. Também é usada pelo [DSSGroupBottomSheet]. */
    internal class Row(context: Context, val item: Item) : FrameLayout(context) {
        private val radio = View(context)
        private val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        private val tagLabel: TextView? = item.tag?.let {
            dssText(context, DSSWeight.MEDIUM, 11f, DSSColors.textPrimary(), maxLines = 1).apply { text = it }
        }
        private val texts = mutableListOf<Pair<TextView, Boolean>>() // (view, secundário?)

        var showsRadio: Boolean = true
            set(value) { field = value; radio.visibility = if (value) VISIBLE else GONE }

        init {
            val ctx = context
            val p = 20f.dpToPx(ctx)
            card.setPadding(p, 16f.dpToPx(ctx), p, 16f.dpToPx(ctx))
            val r = 22f.dpToPx(ctx)
            card.addView(radio, LinearLayout.LayoutParams(r, r).apply { marginEnd = 18f.dpToPx(ctx) })

            val left = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            item.overline?.let { left.addView(text(DSSWeight.REGULAR, 12f, it, secondary = true)) }
            left.addView(text(DSSWeight.BOLD, if (item.overline != null) 22f else 17f, item.title, secondary = false))
            item.subtitle?.let { left.addView(text(DSSWeight.REGULAR, 12f, it, secondary = true)) }
            card.addView(left, weightParams())

            if (item.value != null) {
                val right = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.END
                }
                right.addView(text(DSSWeight.BOLD, 17f, item.value, secondary = false))
                item.valueCaption?.let { right.addView(text(DSSWeight.REGULAR, 12f, it, secondary = true)) }
                card.addView(right, wrapParams().apply { marginStart = 12f.dpToPx(ctx) })
            }
            addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                if (tagLabel != null) topMargin = 9f.dpToPx(ctx)
            })
            tagLabel?.let {
                val h = 6f.dpToPx(ctx)
                it.setPadding(h, 2f.dpToPx(ctx), h, 2f.dpToPx(ctx))
                addView(it, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END).apply {
                    marginEnd = 12f.dpToPx(ctx)
                })
            }
            contentDescription = listOfNotNull(item.overline, item.title, item.subtitle, item.value, item.valueCaption, item.tag)
                .joinToString(", ")
            isClickable = true
            setChecked(false)
        }

        private fun text(weight: DSSWeight, size: Float, value: String, secondary: Boolean): TextView =
            dssText(context, weight, size, if (secondary) DSSColors.textSecondary() else DSSColors.textPrimary()).also {
                it.text = value
                texts += it to secondary
            }

        fun setChecked(checked: Boolean) {
            val strong = DSSColors.textPrimary()
            card.background = DrawableFactory.rounded(
                context,
                if (checked) ColorUtils.setAlphaComponent(strong, 8) else DSSColors.background(),
                10f,
                if (checked) strong else DSSColors.divider(),
                if (checked) 2f else 1.5f,
            )
            radio.background = if (checked) {
                // Rádio marcado: círculo cheio com um anel da cor do fundo por dentro.
                val inset = 2f.dpToPx(context)
                LayerDrawable(arrayOf(oval(strong), oval(strong, DSSColors.background(), 4f)))
                    .apply { setLayerInset(1, inset, inset, inset, inset) }
            } else {
                oval(Color.TRANSPARENT, DSSColors.textTertiary(), 1.5f)
            }
            texts.forEach { (view, secondary) -> view.setTextColor(if (secondary) DSSColors.textSecondary() else DSSColors.textPrimary()) }
            tagLabel?.background = DrawableFactory.rounded(context, DSSColors.background(), 4f, strong, 1f)
            tagLabel?.setTextColor(strong)
            isSelected = checked
        }

        private fun oval(fill: Int, ring: Int? = null, ringDp: Float = 0f) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            if (ring != null) setStroke(ringDp.dpToPx(context), ring)
        }
    }
}
