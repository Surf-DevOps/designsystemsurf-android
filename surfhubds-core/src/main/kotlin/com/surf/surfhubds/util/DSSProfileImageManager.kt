package com.surf.surfhubds.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
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

    fun loadProfileImage(imageView: ImageView, forceRefresh: Boolean = false) {
        val cached = getCachedURL()
        if (!forceRefresh && cached != null) {
            setProfileImage(cached, imageView, forceRefresh = false)
        } else delegate?.profileImageDidStartLoading()
    }

    fun setProfileImage(imageUrl: String?, imageView: ImageView, forceRefresh: Boolean = false) {
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
                    delegate?.profileImageDidFailLoading(e?.localizedMessage ?: "Erro ao carregar imagem.")
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    if (imageUrl != null) setCachedURL(imageUrl)
                    delegate?.profileImageDidFinishLoading(resource)
                    return false
                }
            })
            .into(imageView)
    }

    fun clearImageCache(imageView: ImageView) {
        setCachedURL(null)
        Glide.with(imageView.context).clear(imageView)
    }
}
