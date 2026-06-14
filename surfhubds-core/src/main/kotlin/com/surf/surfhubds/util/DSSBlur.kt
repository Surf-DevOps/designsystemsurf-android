package com.surf.surfhubds.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import eightbitlab.com.blurview.RenderScriptBlur
import kotlin.math.max

/**
 * Blur "real" em todas as versões do Android (minSdk 24).
 *
 * Estratégia: tira um snapshot da janela atual (a Activity por trás do
 * dialog/overlay), reduz a escala e borra o bitmap com o algoritmo da BlurView
 * ([RenderScriptBlur]) — API estável em todas as versões. O resultado é um blur
 * estático, equivalente prático ao `UIVisualEffectView` do iOS para telas modais.
 */
object DSSBlur {

    /** Raio do blur (o clamp do RenderScript é 25). */
    const val DEFAULT_RADIUS = 22f

    /** Escala do snapshot antes de borrar (quanto menor, mais rápido e mais "borrado"). */
    private const val DOWNSCALE = 0.3f

    /** Scrim escuro leve por cima do blur (dá contraste, como o dim do iOS). */
    @ColorInt
    const val DEFAULT_SCRIM: Int = 0x33000000

    /** Desembrulha um [Context] até a [Activity], se houver. */
    fun activityOf(context: Context): Activity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    /** Snapshot da janela da Activity, reduzido e borrado. Null se não der pra capturar. */
    fun blurredSnapshot(activity: Activity, radius: Float = DEFAULT_RADIUS): Bitmap? {
        val decor = activity.window?.decorView ?: return null
        val w = decor.width
        val h = decor.height
        if (w <= 0 || h <= 0) return null
        return try {
            val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(full))
            val sw = max(1, (w * DOWNSCALE).toInt())
            val sh = max(1, (h * DOWNSCALE).toInt())
            val scaled = Bitmap.createScaledBitmap(full, sw, sh, true)
            if (scaled != full) full.recycle()
            RenderScriptBlur(activity).blur(scaled, radius.coerceIn(1f, 25f))
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Drawable de fundo (blur + scrim) pra usar como `window.background` de um dialog.
     * O [BitmapDrawable] é esticado pra preencher a janela.
     */
    fun blurredWindowBackground(
        activity: Activity,
        radius: Float = DEFAULT_RADIUS,
        @ColorInt scrim: Int = DEFAULT_SCRIM,
    ): Drawable? {
        val bmp = blurredSnapshot(activity, radius) ?: return null
        val bd = BitmapDrawable(activity.resources, bmp)
        return LayerDrawable(arrayOf(bd, ColorDrawable(scrim)))
    }

    /**
     * Adiciona um overlay de blur cobrindo a tela inteira (atrás de um alert/loading).
     * Retorna a View adicionada — passe pra [removeBackdrop] no dismiss.
     */
    fun addBlurBackdrop(
        activity: Activity,
        radius: Float = DEFAULT_RADIUS,
        @ColorInt scrim: Int = DEFAULT_SCRIM,
        clickToDismiss: (() -> Unit)? = null,
    ): View? {
        val root = activity.window?.decorView as? ViewGroup ?: return null
        val bmp = blurredSnapshot(activity, radius)
        val overlay = ImageView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.FIT_XY
            if (bmp != null) {
                setImageBitmap(bmp)
                foreground = ColorDrawable(scrim) // scrim POR CIMA do blur
            } else {
                setBackgroundColor(0x99000000.toInt()) // fallback: só escurece
            }
            isClickable = true
            isFocusable = true
            clickToDismiss?.let { action -> setOnClickListener { action() } }
        }
        root.addView(overlay)
        return overlay
    }

    /** Remove um overlay criado por [addBlurBackdrop] e libera o bitmap. */
    fun removeBackdrop(view: View?) {
        view ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        ((view as? ImageView)?.drawable as? BitmapDrawable)?.bitmap?.recycle()
    }
}
