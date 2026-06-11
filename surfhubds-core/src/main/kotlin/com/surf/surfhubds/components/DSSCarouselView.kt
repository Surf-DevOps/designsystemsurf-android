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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.surf.surfhubds.brand.Brand
import com.surf.surfhubds.brand.BrandResolver
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.dpToPx

/**
 * Espelha `DSSCarouselItem` do iOS (`image: UIImage`, `text: String`).
 * `image` é nullable no Android pois a resolução por nome (ImageLoader/getIdentifier)
 * pode não encontrar o recurso; a célula trata `null` sem quebrar.
 */
data class DSSCarouselItem(val image: Drawable?, val text: CharSequence)

class DSSCarouselView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), ThemeAware {

    private val pager = ViewPager2(context)
    private val pageControl = PageControlView(context)

    private val items = mutableListOf<DSSCarouselItem>()
    private var textColor: Int = Color.BLACK
    private var textTypeface: Typeface? = null

    private var currentIndex: Int = 0

    /** Equivalente ao `indexDidChange` (PassthroughSubject<Int>) do iOS. */
    var indexDidChange: ((Int) -> Unit)? = null

    /**
     * Cor de background customizada. Se `null`, usa `DSSColors.surface()`.
     * Equivalente ao `customBackgroundColor` do iOS.
     */
    var customBackgroundColor: Int? = null
        set(value) {
            field = value
            applyColors()
        }

    val getCurrentIndex: Int get() = currentIndex

    init {
        orientation = VERTICAL
        addView(
            pager,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 450.dpToPx(context)).apply {
                bottomMargin = 10.dpToPx(context)
            },
        )
        addView(
            pageControl,
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        pager.adapter = CarouselAdapter()
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentIndex = position
                pageControl.setCurrentPage(position)
            }
        })
        applyColors()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) {
        applyColors()
        pageControl.refresh()
    }

    private fun applyColors() {
        val bg = customBackgroundColor ?: DSSColors.surface()
        setBackgroundColor(bg)
    }

    fun configure(items: List<DSSCarouselItem>, textColor: Int = Color.BLACK, textTypeface: Typeface? = null) {
        this.items.clear()
        this.items.addAll(items)
        this.textColor = textColor
        this.textTypeface = textTypeface
        pageControl.setPageCount(items.size)
        pager.adapter?.notifyDataSetChanged()
        applyColors()
    }

    fun goToNextItem(animated: Boolean = true) {
        if (items.isEmpty()) return

        if (currentIndex < items.size - 1) {
            val nextIndex = currentIndex + 1
            currentIndex = nextIndex
            pager.setCurrentItem(nextIndex, animated)
        }

        // iOS: sempre emite o índice atual para os listeners.
        indexDidChange?.invoke(currentIndex)
    }

    private inner class CarouselAdapter : RecyclerView.Adapter<CarouselVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselVH {
            val cell = CarouselCellView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            return CarouselVH(cell)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: CarouselVH, position: Int) {
            holder.cell.configure(items[position], textColor, textTypeface)
        }
    }

    private class CarouselVH(val cell: CarouselCellView) : RecyclerView.ViewHolder(cell)

    private class CarouselCellView(context: Context) : FrameLayout(context) {
        private val imageView = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        private val label = TextView(context).apply {
            textSize = 12f
            typeface = DSSFont.light(context, 12f).typeface
            // iOS: numberOfLines = 0 (ilimitado). No Android, maxLines = 0 esconde o texto;
            // o equivalente a "ilimitado" é Int.MAX_VALUE.
            maxLines = Int.MAX_VALUE
            setTextColor(DSSColors.textPrimary())
            gravity = Gravity.CENTER
        }

        init {
            // iOS: UIStackView(axis: .vertical, alignment: .center, spacing: 10), centrado em Y.
            val stack = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            stack.addView(imageView, LinearLayout.LayoutParams(300.dpToPx(context), 300.dpToPx(context)))
            stack.addView(
                label,
                LinearLayout.LayoutParams(350.dpToPx(context), 100.dpToPx(context)).apply {
                    topMargin = 10.dpToPx(context)
                },
            )
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                leftMargin = 20.dpToPx(context)
                rightMargin = 20.dpToPx(context)
            }
            addView(stack, lp)
        }

        fun configure(item: DSSCarouselItem, textColor: Int, typeface: Typeface?) {
            imageView.setImageDrawable(item.image)
            label.text = item.text
            label.setTextColor(textColor)
            typeface?.let { label.typeface = it }
            // iOS: brand .flachip alinha à esquerda; demais centralizam.
            label.gravity = if (BrandResolver.current(context) == Brand.FLACHIP) {
                Gravity.START or Gravity.CENTER_VERTICAL
            } else {
                Gravity.CENTER
            }
        }
    }

    private class PageControlView(context: Context) : LinearLayout(context) {
        private var count: Int = 0
        private var currentPage: Int = 0
        private val dotSize = 8.dpToPx(context)
        private val dotSpacing = 6.dpToPx(context)

        init { orientation = HORIZONTAL }

        fun setPageCount(count: Int) { this.count = count; rebuild() }
        fun setCurrentPage(page: Int) { currentPage = page; rebuild() }

        /** Reaplica cores dos dots (troca de tema). */
        fun refresh() = rebuild()

        private fun rebuild() {
            removeAllViews()
            repeat(count) { i ->
                val dot = View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(if (i == currentPage) DSSColors.primary() else Color.LTGRAY)
                    }
                }
                addView(
                    dot,
                    LayoutParams(dotSize, dotSize).apply {
                        leftMargin = if (i == 0) 0 else dotSpacing
                    },
                )
            }
        }
    }
}
