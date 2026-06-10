package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSBannerCollectionView` do iOS — carrossel horizontal de banners.
 */
class DSSBannerCollectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    /** Modelo equivalente ao `BannerItem` do iOS. */
    data class BannerItem(val id: Int, val url: String, val value: String? = null)

    private val bannerWidthDp = 314f
    private val bannerHeightDp = 106f
    private val spacingDp = 16f

    private val items = mutableListOf<BannerItem>()
    private var tapAction: ((Int) -> Unit)? = null

    private val recyclerView: RecyclerView = RecyclerView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        clipToPadding = false
        val pad = spacingDp.dpToPx(context)
        setPadding(pad, 0, pad, 0)
    }

    private val adapter = BannerAdapter()

    init {
        addView(
            recyclerView,
            LayoutParams(LayoutParams.MATCH_PARENT, bannerHeightDp.dpToPx(context)),
        )
        recyclerView.adapter = adapter
        refresh()
        setupThemeObserver()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() {
        // Banner é transparente — segue o pai. Nada a fazer hoje.
    }

    /** Configura com lista de URLs. */
    fun configure(urls: List<String>, tapAction: ((Int) -> Unit)? = null) {
        items.clear()
        urls.forEachIndexed { index, url ->
            items.add(BannerItem(id = index, url = url, value = null))
        }
        this.tapAction = tapAction
        adapter.notifyDataSetChanged()
    }

    private inner class BannerAdapter : RecyclerView.Adapter<BannerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
            val cell = BannerCellView(parent.context)
            return BannerViewHolder(cell)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
            val lp = (holder.itemView.layoutParams as? RecyclerView.LayoutParams)
                ?: RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
            lp.width = bannerWidthDp.dpToPx(context)
            lp.height = bannerHeightDp.dpToPx(context)
            if (position < items.size - 1) lp.rightMargin = spacingDp.dpToPx(context)
            holder.itemView.layoutParams = lp

            holder.cell.load(items[position].url)
            holder.itemView.setOnClickListener { tapAction?.invoke(position) }
        }
    }

    private class BannerViewHolder(val cell: BannerCellView) : RecyclerView.ViewHolder(cell)

    /** Cell com imagem (Glide) + indicador de loading. */
    private class BannerCellView(context: Context) : FrameLayout(context) {
        private val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = DrawableFactory.rounded(
                context = context,
                backgroundColor = DSSColors.backgroundSecondary(),
                cornerRadiusDp = 8f,
            )
            clipToOutline = true
        }
        private val loading = ProgressBar(context)

        init {
            addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            val lpLoad = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lpLoad.gravity = android.view.Gravity.CENTER
            addView(loading, lpLoad)
        }

        fun load(url: String) {
            loading.visibility = VISIBLE
            Glide.with(context).load(url)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean,
                    ): Boolean { loading.visibility = GONE; return false }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean,
                    ): Boolean { loading.visibility = GONE; return false }
                })
                .into(imageView)
        }
    }
}
