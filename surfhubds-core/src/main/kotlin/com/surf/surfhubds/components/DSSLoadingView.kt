package com.surf.surfhubds.components

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.surf.surfhubds.brand.Brand
import com.surf.surfhubds.brand.BrandResolver
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSLoadingView.shared` do iOS.
 *
 * Singleton — `DSSLoadingView.startLoading(activity)` / `stopLoading()`.
 *
 * Espelha o iOS: cada brand tem um GIF animado (`gif_<brand>`, ex. `gif_matizconecta`)
 * embarcado em `res/raw/` do módulo `surfhubds-brand-*`. A resolução segue a mesma
 * cadeia de fallback do iOS:
 * 1. GIF específico da brand (`gif_<brand>`);
 * 2. GIF genérico (`gif`);
 * 3. `ProgressBar` indeterminado tintado com a primary da brand (equivalente ao
 *    `useSimpleLoadingIndicator()` do iOS).
 *
 * Opcionalmente a brand pode registrar uma View custom via [setLoadingViewFactory]
 * (extensão Android; tem prioridade sobre a cadeia acima).
 *
 * Nota de plataforma: o iOS aplica um `UIVisualEffectView` (blur) por baixo do GIF
 * quando `SurfHubDS.blurEnabled`. No Android o fundo é a cor semitransparente
 * `DSSColors.overlay()`, equivalente visual mais próximo sem dependência de blur.
 */
object DSSLoadingView {

    /** Tamanho do GIF, espelha os 80pt do iOS. */
    private const val INDICATOR_SIZE_DP = 80f

    /** Cor literal `.systemBlue` do iOS usada no indicador de fallback (não é token semântico). */
    private const val SYSTEM_BLUE = 0xFF007AFF.toInt()

    private var overlay: View? = null
    private var factory: ((Context) -> View)? = null

    /**
     * Brand pode registrar uma factory custom (ex.: GIF/Lottie animado).
     * Configure no `Application.onCreate()` se quiser substituir o default.
     * Tem prioridade sobre a resolução automática do GIF da brand.
     */
    fun setLoadingViewFactory(factory: (Context) -> View) {
        this.factory = factory
    }

    fun startLoading(activity: Activity) {
        if (overlay != null) return
        val root = activity.window.decorView as ViewGroup

        val container = FrameLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(DSSColors.overlay())
            isClickable = true
            isFocusable = true
        }

        val content = factory?.invoke(activity) ?: loadingContent(activity)
        val contentLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ).apply { gravity = Gravity.CENTER }

        container.addView(content, contentLp)
        root.addView(container)
        overlay = container
    }

    fun stopLoading() {
        val v = overlay ?: return
        (v.parent as? ViewGroup)?.removeView(v)
        overlay = null
    }

    /**
     * Espelha o `loadGif()` do iOS: tenta o GIF específico da brand, depois o GIF
     * genérico, e por fim cai para o `ProgressBar` (= `useSimpleLoadingIndicator`).
     */
    private fun loadingContent(context: Context): View {
        val gifRes = resolveGifRes(context)
        return if (gifRes != 0) gifImageView(context, gifRes) else defaultProgress(context)
    }

    /**
     * Resolve o `res/raw/` do GIF seguindo a cadeia do iOS:
     * `gif_<brand>` → `gif`. Retorna 0 se nenhum existir.
     */
    private fun resolveGifRes(context: Context): Int {
        val brand = BrandResolver.current(context)
        val pkg = context.packageName
        val brandGifName = brandSpecificGifName(brand)
        val branded = context.resources.getIdentifier(brandGifName, "raw", pkg)
        if (branded != 0) return branded
        return context.resources.getIdentifier("gif", "raw", pkg)
    }

    /**
     * Espelha `getBrandSpecificGifName(_:)` do iOS — nome do GIF por brand (`gif_<brand>`).
     */
    private fun brandSpecificGifName(brand: Brand): String = when (brand) {
        Brand.MATIZCONECTA -> "gif_matizconecta"
        Brand.IFOOD -> "gif_ifood"
        Brand.BANDSPORTS -> "gif_bandsports"
        Brand.DEFAULT -> "gif_default"
        Brand.FLACHIP -> "gif_flachip"
        Brand.CONECTA -> "gif_conecta"
        Brand.MEGA -> "gif_mega"
        Brand.FLUXO -> "gif_fluxo"
        Brand.PAFER -> "gif_pafer"
        Brand.PAGUEMENOS -> "gif_paguemenos"
        Brand.CARREFOURCHIP -> "gif_carrefourchip"
        Brand.CORREIOSCELULAR -> "gif_correioscelular"
        Brand.PERNAMBUCANASCHIP -> "gif_pernambucanaschip"
        Brand.UBER -> "gif_uber"
    }

    /** GIF animado 80x80, `scaleAspectFit` → `FIT_CENTER`, carregado via Glide. */
    private fun gifImageView(context: Context, gifRes: Int): View {
        val px = INDICATOR_SIZE_DP.dpToPx(context)
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = ViewGroup.LayoutParams(px, px)
        }
        Glide.with(context).asGif().load(gifRes).into(imageView)
        return imageView
    }

    /** Fallback final, equivalente ao `useSimpleLoadingIndicator()` do iOS. */
    private fun defaultProgress(context: Context): View {
        val bar = ProgressBar(context)
        // iOS: activityIndicator.color = .systemBlue (cor literal do sistema, #007AFF).
        bar.indeterminateTintList = android.content.res.ColorStateList.valueOf(SYSTEM_BLUE)
        // iOS não define tamanho explícito no UIActivityIndicatorView(style: .large) — sizes-to-fit.
        bar.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        return bar
    }
}
