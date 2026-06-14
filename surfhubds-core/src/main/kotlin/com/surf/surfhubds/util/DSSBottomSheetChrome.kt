package com.surf.surfhubds.util

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.graphics.Outline
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Aplica o "chrome" padrão do DS em TODOS os [BottomSheetDialogFragment], sem precisar
 * editar cada um:
 * - cantos SUPERIORES arredondados (via outline + clipToOutline);
 * - blur na área que não é o sheet (snapshot borrado como fundo da janela);
 * - toque fora do sheet faz dismiss.
 *
 * É ligado automaticamente em [com.surf.surfhubds.SurfHubDS.initialize] via
 * [register]. Não depende de herança — usa [FragmentManager.FragmentLifecycleCallbacks].
 */
object DSSBottomSheetChrome {

    /** Raio dos cantos superiores (iOS usa 24). */
    var cornerRadiusDp: Float = 24f

    /** Liga o chrome automático para todos os sheets das Activities do app. */
    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                (activity as? FragmentActivity)?.supportFragmentManager
                    ?.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private val fragmentCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
            if (f !is BottomSheetDialogFragment) return
            // post: garante que o dialog/janela e o sheet já estão prontos e medidos.
            f.view?.post { apply(f) } ?: apply(f)
        }
    }

    /** Aplica o chrome num sheet específico (idempotente). */
    fun apply(fragment: BottomSheetDialogFragment) {
        val dialog = fragment.dialog as? BottomSheetDialog ?: return
        val window = dialog.window
        val activity = fragment.activity ?: return

        dialog.setCanceledOnTouchOutside(true)

        // --- Cantos superiores arredondados (clip do sheet inteiro) ---
        // (idempotente: re-aplicar outline/clip é inofensivo.)
        val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (sheet != null) {
            val r = cornerRadiusDp.dpToPxFloat(sheet.context)
            sheet.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    // estende o retângulo r px abaixo da base → só os cantos de cima ficam arredondados.
                    outline.setRoundRect(0, 0, view.width, (view.height + r).toInt(), r)
                }
            }
            sheet.clipToOutline = true
            // o container atrás do sheet fica transparente p/ o blur da janela aparecer.
            (sheet.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
        }

        // --- Blur atrás do sheet (na área clicável que faz dismiss) ---
        if (window != null) {
            DSSBlur.blurredWindowBackground(activity)?.let { bg ->
                window.setBackgroundDrawable(bg)
                window.setDimAmount(0f) // o scrim já vem embutido no blur
            }
        }
    }
}
