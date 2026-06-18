package com.surf.surfhubds.util

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * Port do `DSSProfileImageManager.swift` — carrega imagem de perfil via Glide
 * (equivalente ao SDWebImage do iOS), com cache opcional e callback de status.
 */
class DSSProfileImageManager(
    private val getCachedURL: () -> String?,
    private val setCachedURL: (String?) -> Unit,
) {

    interface Delegate {
        fun profileImageDidStartLoading()
        fun profileImageDidFinishLoading(drawable: Drawable?)
        fun profileImageDidFailLoading(error: String)
    }

    var delegate: Delegate? = null

    /**
     * Application context capturado no último load — usado pelo [clearImageCache]
     * no-arg, já que `Glide.get(...).clearMemory()` exige um Context (no iOS o
     * `SDImageCache.shared` é um singleton global sem essa dependência).
     */
    private var appContext: android.content.Context? = null

    fun loadProfileImage(imageView: ImageView, forceRefresh: Boolean = false) {
        val cached = getCachedURL()
        if (!forceRefresh && cached != null) {
            setProfileImage(cached, imageView, forceRefresh = false, showLoading = false)
        } else {
            showLoadingIndicator(imageView)
            delegate?.profileImageDidStartLoading()
        }
    }

    fun setProfileImage(
        imageUrl: String?,
        imageView: ImageView,
        forceRefresh: Boolean = false,
        showLoading: Boolean = true,
    ) {
        appContext = imageView.context.applicationContext
        if (showLoading) showLoadingIndicator(imageView)
        delegate?.profileImageDidStartLoading()
        Glide.with(imageView.context)
            .load(imageUrl)
            .diskCacheStrategy(if (forceRefresh) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC)
            .skipMemoryCache(forceRefresh)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    removeLoadingIndicator(imageView)
                    delegate?.profileImageDidFailLoading(e?.localizedMessage ?: AppStrings.brand(imageView.context, "profile_image_load_error", "Erro ao carregar imagem."))
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    removeLoadingIndicator(imageView)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    if (imageUrl != null) setCachedURL(imageUrl)
                    delegate?.profileImageDidFinishLoading(resource)
                    return false
                }
            })
            .into(imageView)
    }

    /**
     * Espelha o `clearImageCache()` do iOS (no-arg): limpa a URL em cache e a
     * memória do Glide (`SDImageCache.shared.clearMemory()` →
     * `Glide.get(context).clearMemory()`). Usa o [appContext] capturado no último
     * load; se nenhum load ocorreu ainda, apenas zera a URL em cache.
     */
    fun clearImageCache() {
        setCachedURL(null)
        appContext?.let { Glide.get(it).clearMemory() }
    }

    /**
     * Espelha `setupDefaultProfileImage(in:)` do iOS — placeholder de pessoa,
     * `scaleAspectFill` (→ `CENTER_CROP`) e `clipsToBounds`.
     */
    fun setupDefaultProfileImage(imageView: ImageView) {
        imageView.setImageResource(android.R.drawable.sym_contact_card)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.clipToOutline = true
    }

    /**
     * Espelha `prepareImageView(_:)` do iOS — `clipsToBounds` + fundo neutro
     * (`UIColor.systemGray5` → token semântico `backgroundSecondary`).
     */
    fun prepareImageView(imageView: ImageView) {
        imageView.clipToOutline = true
        imageView.setBackgroundColor(com.surf.surfhubds.theme.DSSColors.backgroundSecondary())
    }

    // MARK: - Loading indicator (espelha o UIActivityIndicatorView overlay do iOS)

    private fun showLoadingIndicator(imageView: ImageView) {
        removeLoadingIndicator(imageView)
        val parent = imageView.parent as? ViewGroup ?: return
        val indicator = ProgressBar(imageView.context).apply {
            id = LOADING_INDICATOR_ID
            // iOS: activityIndicator.color = .systemBlue (cor literal do sistema, #007AFF).
            indeterminateTintList = ColorStateList.valueOf(SYSTEM_BLUE)
        }
        // Se o parent for FrameLayout, centraliza via gravity; senão, posiciona por
        // coordenadas sobre a imageView (espelha o center constraint do iOS).
        if (parent is FrameLayout) {
            indicator.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER }
            parent.addView(indicator)
        } else {
            indicator.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            parent.addView(indicator)
            indicator.post {
                indicator.x = imageView.x + (imageView.width - indicator.width) / 2f
                indicator.y = imageView.y + (imageView.height - indicator.height) / 2f
            }
        }
    }

    private fun removeLoadingIndicator(imageView: ImageView) {
        val parent = imageView.parent as? ViewGroup ?: return
        parent.findViewById<ProgressBar>(LOADING_INDICATOR_ID)?.let { parent.removeView(it) }
    }

    private companion object {
        /** Tag/id do indicador de loading (espelha o `tag = 999` do iOS). */
        const val LOADING_INDICATOR_ID = 999

        /** Cor literal `.systemBlue` do iOS (#007AFF) — não é token semântico. */
        const val SYSTEM_BLUE = 0xFF007AFF.toInt()
    }
}
