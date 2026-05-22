package com.surf.surfhubds.components

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.surf.surfhubds.theme.DSSColors

/**
 * Port do `DSSLoadingView.shared` do iOS.
 *
 * Singleton — `DSSLoadingView.startLoading(activity)` / `stopLoading()`.
 *
 * No iOS, cada brand tem um `gif_*.gif`. Aqui, brand pode passar uma View custom via
 * [setLoadingViewFactory]. Sem factory, mostra um `ProgressBar` tintado com a primary da brand.
 */
object DSSLoadingView {

    private var overlay: View? = null
    private var factory: ((Context) -> View)? = null

    /**
     * Brand pode registrar uma factory custom (ex.: GIF/Lottie animado).
     * Configure no `Application.onCreate()` se quiser substituir o default.
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

        val content = factory?.invoke(activity) ?: defaultProgress(activity)
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

    private fun defaultProgress(context: Context): View {
        val bar = ProgressBar(context)
        bar.indeterminateTintList = android.content.res.ColorStateList.valueOf(DSSColors.primary())
        val px = (80f * context.resources.displayMetrics.density).toInt()
        bar.layoutParams = ViewGroup.LayoutParams(px, px)
        return bar
    }
}
