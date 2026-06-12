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
    data class BannerItem(
        val id: Int,
        val url: String,
        val value: String? = null,
        /** Caminho/destino retornado pela API; é a string disparada no clique do banner. */
        val path: String? = null,
    )

    /**
     * Modelo agnóstico equivalente ao `PreSignedDownloadListSuccess.PresignedUrl` do iOS
     * (que vive no SurfAPIKit). Usado pela sobrecarga de [configure] que dispara o `path`.
     */
    data class PresignedBanner(
        val id: Int,
        val url: String,
        val value: String? = null,
        val path: String? = null,
    )

    private val bannerWidthDp = 314f
    private val bannerHeightDp = 106f
    private val spacingDp = 16f

    private val items = mutableListOf<BannerItem>()
    private var tapAction: ((Int) -> Unit)? = null

    /** Handler chamado ao tocar no banner; recebe a string do `path` retornado pela API. */
    private var pathTapAction: ((String) -> Unit)? = null

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
        sortByBannerOrder()
        this.tapAction = tapAction
        this.pathTapAction = null

        // Esconde a view quando não há banners (lista vazia / erro).
        visibility = if (items.isEmpty()) GONE else VISIBLE

        preloadImages(items.map { it.url })

        adapter.notifyDataSetChanged()
    }

    /**
     * Configura os banners a partir da lista de [PresignedBanner] (equivalente à sobrecarga
     * iOS que recebe `[PreSignedDownloadListSuccess.PresignedUrl]`). Exibe quantos banners
     * vierem na lista. Ao tocar num banner, o [onBannerTap] dispara a string do `path` daquele item.
     */
    fun configureBanners(banners: List<PresignedBanner>, onBannerTap: ((String) -> Unit)? = null) {
        items.clear()
        banners.forEach {
            items.add(BannerItem(id = it.id, url = it.url, value = it.value, path = it.path))
        }
        sortByBannerOrder()
        this.tapAction = null
        this.pathTapAction = onBannerTap

        // Esconde a view quando não há banners (lista vazia / erro).
        visibility = if (items.isEmpty()) GONE else VISIBLE

        preloadImages(items.map { it.url })

        adapter.notifyDataSetChanged()
    }

    /**
     * Ordena os banners pela sequência `banner_N` retornada pela API (ex.: banner_1,
     * banner_2, ...). Usa o `value` quando presente (ex.: "banner_1.png"); senão extrai
     * de dentro da própria URL presigned, que carrega o nome do objeto. Itens sem índice
     * vão para o fim, preservando a ordem original (sort estável).
     */
    private fun sortByBannerOrder() {
        items.sortBy { bannerOrder(it.value ?: it.url) }
    }

    private fun bannerOrder(source: String): Int =
        Regex("banner_(\\d+)").find(source)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE

    private fun preloadImages(urls: List<String>) {
        urls.forEach { url ->
            Glide.with(context).load(url).preload()
        }
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
            holder.itemView.setOnClickListener {
                tapAction?.invoke(position)

                // Dispara a string do path do banner tocado.
                if (position < items.size) {
                    val path = items[position].path
                    if (!path.isNullOrEmpty()) {
                        pathTapAction?.invoke(path)
                    }
                }
            }
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

        // Espelha SDImageResizingTransformer(size: 314x106) do iOS.
        private val bannerWidthPx = 314f.dpToPx(context)
        private val bannerHeightPx = 106f.dpToPx(context)

        init {
            addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            val lpLoad = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lpLoad.gravity = android.view.Gravity.CENTER
            addView(loading, lpLoad)
        }

        fun load(url: String) {
            loading.visibility = VISIBLE
            // Reset (equivale ao prepareForReuse do iOS): limpa imagem e volta o fundo placeholder.
            imageView.setImageDrawable(null)
            imageView.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = DSSColors.backgroundSecondary(),
                cornerRadiusDp = 8f,
            )
            Glide.with(context).load(url)
                .override(bannerWidthPx, bannerHeightPx)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean,
                    ): Boolean {
                        loading.visibility = GONE
                        // iOS: imageView.backgroundColor = .systemGray5 no erro.
                        imageView.background = DrawableFactory.rounded(
                            context = context,
                            backgroundColor = DSSColors.backgroundSecondary(),
                            cornerRadiusDp = 8f,
                        )
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        loading.visibility = GONE
                        // iOS: fade-in (alpha 0 -> 1, 0.3s) só quando vem da rede (cacheType == .none).
                        if (dataSource != com.bumptech.glide.load.DataSource.MEMORY_CACHE &&
                            dataSource != com.bumptech.glide.load.DataSource.RESOURCE_DISK_CACHE &&
                            dataSource != com.bumptech.glide.load.DataSource.DATA_DISK_CACHE
                        ) {
                            imageView.alpha = 0f
                            imageView.animate().alpha(1f).setDuration(300).start()
                        } else {
                            imageView.alpha = 1f
                        }
                        return false
                    }
                })
                .into(imageView)
        }
    }
}
