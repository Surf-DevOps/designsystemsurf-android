package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.surf.surfhubds.brand.Brand
import com.surf.surfhubds.brand.BrandResolver
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/** Port do `DSSCarouselItem` do iOS. Item exibido em [DSSCarouselView]. */
data class DSSCarouselItem(val image: Drawable?, val text: CharSequence)

/**
 * Port do `DSSCarouselView` do iOS — carrossel com paging horizontal + page control.
 * Inclui o cell (DSSCarouselCollectionViewCell) como inner class.
 */
class DSSCarouselView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    private val recyclerView = RecyclerView(context)
    private val pageControl = PageControlView(context)

    private val items = mutableListOf<DSSCarouselItem>()
    private val adapter = CarouselAdapter()

    private var textColor: Int = Color.BLACK
    private var textTypeface: Typeface? = null
    private var textSizeSp: Float = 12f

    /** Callback chamado quando o índice atual muda (via scroll ou programático). */
    var onIndexChanged: ((Int) -> Unit)? = null

    private var currentIndex: Int = 0
        set(value) {
            if (field == value) return
            field = value
            pageControl.setCurrentPage(value)
            onIndexChanged?.invoke(value)
        }

    val getCurrentIndex: Int get() = currentIndex

    init {
        orientation = VERTICAL
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        recyclerView.adapter = adapter
        PagerSnapHelper().attachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = rv.layoutManager as LinearLayoutManager
                    val first = lm.findFirstCompletelyVisibleItemPosition()
                    if (first != RecyclerView.NO_POSITION) currentIndex = first
                }
            }
        })

        addView(
            recyclerView,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 450f.dpToPx(context)),
        )
        addView(
            pageControl,
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10f.dpToPx(context)
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        setBackgroundColor(DSSColors.surface())
        recyclerView.setBackgroundColor(DSSColors.surface())
    }

    fun configure(items: List<DSSCarouselItem>, textColor: Int = Color.BLACK, textTypeface: Typeface? = null) {
        this.items.clear()
        this.items.addAll(items)
        this.textColor = textColor
        this.textTypeface = textTypeface
        pageControl.setPageCount(items.size)
        adapter.notifyDataSetChanged()
    }

    fun goToNextItem(animated: Boolean = true) {
        if (items.isEmpty()) return
        if (currentIndex < items.size - 1) {
            val next = currentIndex + 1
            if (animated) recyclerView.smoothScrollToPosition(next) else recyclerView.scrollToPosition(next)
            currentIndex = next
        } else {
            onIndexChanged?.invoke(currentIndex)
        }
    }

    private inner class CarouselAdapter : RecyclerView.Adapter<CarouselViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
            val cell = CarouselCellView(parent.context)
            return CarouselViewHolder(cell)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
            val lp = holder.itemView.layoutParams as RecyclerView.LayoutParams
            lp.width = recyclerView.width.takeIf { it > 0 } ?: ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = recyclerView.height.takeIf { it > 0 } ?: ViewGroup.LayoutParams.MATCH_PARENT
            holder.itemView.layoutParams = lp
            holder.cell.configure(
                item = items[position],
                textColor = textColor,
                typeface = textTypeface,
            )
        }
    }

    private class CarouselViewHolder(val cell: CarouselCellView) : RecyclerView.ViewHolder(cell)

    /** Port do `DSSCarouselCollectionViewCell` do iOS. */
    private class CarouselCellView(context: Context) : FrameLayout(context), ThemeAware {
        private val imageView = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        private val label = TextView(context).apply {
            textSize = 12f
            typeface = DSSFont.light(context, 12f).typeface
            maxLines = 0
        }

        init {
            val stack = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            stack.addView(
                imageView,
                LinearLayout.LayoutParams(300f.dpToPx(context), 300f.dpToPx(context)),
            )
            stack.addView(
                label,
                LinearLayout.LayoutParams(350f.dpToPx(context), 100f.dpToPx(context)).apply {
                    topMargin = 10f.dpToPx(context)
                },
            )
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                leftMargin = 20f.dpToPx(context)
                rightMargin = 20f.dpToPx(context)
            }
            addView(stack, lp)
            applyColors()
            setupThemeObserver()
        }

        override fun applyTheme(theme: Theme) { applyColors() }

        private fun applyColors() {
            setBackgroundColor(DSSColors.surface())
        }

        fun configure(item: DSSCarouselItem, textColor: Int, typeface: Typeface?) {
            imageView.setImageDrawable(item.image)
            label.text = item.text
            label.setTextColor(textColor)
            typeface?.let { label.typeface = it }
            label.textAlignment = if (BrandResolver.current(context) == Brand.FLACHIP)
                View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_CENTER
        }
    }

    /** Implementação simples de PageControl — bolinhas horizontais. */
    private class PageControlView(context: Context) : LinearLayout(context), ThemeAware {
        private var count: Int = 0
        private var currentPage: Int = 0
        private val dotSize = 8f.dpToPx(context)
        private val dotSpacing = 6f.dpToPx(context)

        init {
            orientation = HORIZONTAL
            setupThemeObserver()
        }

        override fun applyTheme(theme: Theme) { rebuild() }

        fun setPageCount(count: Int) { this.count = count; rebuild() }

        fun setCurrentPage(page: Int) { currentPage = page; rebuild() }

        private fun rebuild() {
            removeAllViews()
            repeat(count) { i ->
                val dot = View(context)
                dot.background = circle(if (i == currentPage) DSSColors.primary() else Color.LTGRAY)
                addView(
                    dot,
                    LayoutParams(dotSize, dotSize).apply {
                        leftMargin = if (i == 0) 0 else dotSpacing
                    },
                )
            }
        }

        private fun circle(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }
}
