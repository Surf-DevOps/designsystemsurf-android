package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.surf.surfhubds.components.DSSCarouselItem
import com.surf.surfhubds.theme.DSSColors



data class DSSCarouselItem(val image: Drawable?, val text: CharSequence)

class DSSCarouselView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val pager = ViewPager2(context)
    private val pageControl = PageControlView(context)

    private val items = mutableListOf<DSSCarouselItem>()
    private var textColor: Int = Color.BLACK
    private var textTypeface: Typeface? = null

    val getCurrentIndex: Int get() = pager.currentItem

    init {
        orientation = VERTICAL
        addView(
            pager,
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0).apply { weight = 1f },
        )
        addView(
            pageControl,
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 10.dp
                bottomMargin = 10.dp
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        pager.adapter = CarouselAdapter()
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { pageControl.setCurrentPage(position) }
        })
    }

    fun configure(items: List<DSSCarouselItem>, textColor: Int = Color.BLACK, textTypeface: Typeface? = null) {
        this.items.clear()
        this.items.addAll(items)
        this.textColor = textColor
        this.textTypeface = textTypeface
        pageControl.setPageCount(items.size)
        pager.adapter?.notifyDataSetChanged()
        pageControl.post { pageControl.setCurrentPage(pager.currentItem) }
    }

    fun goToNextItem(animated: Boolean = true) {
        if (items.isEmpty() || pager.currentItem >= items.size - 1) return
        pager.setCurrentItem(pager.currentItem + 1, animated)
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
            textSize = 14f
            maxLines = 0
            gravity = Gravity.CENTER
        }

        init {
            val stack = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            stack.addView(imageView, LinearLayout.LayoutParams(300.dp(context), 300.dp(context)))
            stack.addView(
                label,
                LinearLayout.LayoutParams(350.dp(context), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = 10.dp(context)
                },
            )
            val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                leftMargin = 20.dp(context)
                rightMargin = 20.dp(context)
            }
            addView(stack, lp)
        }

        fun configure(item: DSSCarouselItem, textColor: Int, typeface: Typeface?) {
            imageView.setImageDrawable(item.image)
            label.text = item.text
            label.setTextColor(textColor)
            typeface?.let { label.typeface = it }
        }
    }

    private class PageControlView(context: Context) : LinearLayout(context) {
        private var count: Int = 0
        private var currentPage: Int = 0
        private val dotSize = 8.dp(context)
        private val dotSpacing = 6.dp(context)

        init {
            orientation = HORIZONTAL
            minimumHeight = dotSize
        }

        fun setPageCount(count: Int) {
            if (this.count == count) {
                refreshColors()
                return
            }
            this.count = count
            if (currentPage >= count) currentPage = 0
            rebuild()
        }

        fun setCurrentPage(page: Int) {
            if (currentPage == page && childCount == count) {
                refreshColors()
                return
            }
            currentPage = page
            if (childCount != count) rebuild() else refreshColors()
        }

        private fun rebuild() {
            removeAllViews()
            repeat(count) { i ->
                val dot = View(context).apply { background = makeDot(i == currentPage) }
                addView(
                    dot,
                    LayoutParams(dotSize, dotSize).apply {
                        leftMargin = if (i == 0) 0 else dotSpacing
                    },
                )
            }
            requestLayout()
            invalidate()
        }

        private fun refreshColors() {
            for (i in 0 until childCount) {
                getChildAt(i).background = makeDot(i == currentPage)
            }
        }

        private fun makeDot(active: Boolean): GradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (active) DSSColors.primary() else Color.LTGRAY)
        }
    }

    private companion object {
        private val Int.dp: Int get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
        private fun Int.dp(ctx: Context): Int =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), ctx.resources.displayMetrics).toInt()
    }
}
