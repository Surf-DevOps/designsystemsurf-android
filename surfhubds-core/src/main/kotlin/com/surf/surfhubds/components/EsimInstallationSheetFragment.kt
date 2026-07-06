package com.surf.surfhubds.components

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.dpToPxFloat

/**
 * Port do `EsimInstallationSheetViewController` do iOS.
 *
 * `BottomSheetDialogFragment` que apresenta o [BottomSheetEsimInstallationView].
 * Equivalente ao sheet `pageSheet` com `medium` detent do iOS.
 *
 * Para apresentar:
 * ```
 * EsimInstallationSheetFragment.present(
 *     fragmentManager,
 *     text = "Texto de autorização",
 *     onDismiss = { /* fechado sem aceitar */ },
 *     accepted = { /* clicou Concordar */ },
 * )
 * ```
 */
class EsimInstallationSheetFragment : BottomSheetDialogFragment() {

    var onDismissed: (() -> Unit)? = null
    var accepted: (() -> Unit)? = null

    private var contentText: String = ""
    private var notifyDismiss: Boolean = true
    private var didFireDismiss: Boolean = false

    fun setContentText(text: String) { contentText = text }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        contentText = arguments?.getString(ARG_TEXT, "") ?: contentText

        val content = BottomSheetEsimInstallationView(ctx).apply {
            configure(contentText)
            onContinueTap = {
                notifyDismiss = false
                accepted?.invoke()
                dismissAllowingStateLoss()
            }
            onCancelTap = {
                notifyDismiss = true
                dismissAllowingStateLoss()
            }
        }

        // iOS: `preferredCornerRadius = 16` -> cantos superiores arredondados em 16dp
        // (cantos inferiores ficam contra a borda da tela). O background usa o mesmo
        // esquema de cores do iOS (`setupSheet`).
        val root = android.widget.FrameLayout(ctx).apply {
            background = sheetBackground()
        }
        root.addView(content, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
        ))

        // iOS: `prefersGrabberVisible = true` -> grabber/drag handle visível no topo.
        val grabber = BottomSheetDragHandleView(ctx)
        root.addView(grabber, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL,
        ))

        return root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet,
            ) ?: return@setOnShowListener
            // iOS: `preferredCornerRadius = 16` aplicado ao próprio sheet.
            sheet.background = sheetBackground()
            // O sheet abraça o conteúdo (sem espaço vazio embaixo) e abre já
            // totalmente expandido — em vez de um detent fixo mais alto que o conteúdo.
            sheet.layoutParams = sheet.layoutParams.apply {
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            BottomSheetBehavior.from(sheet).apply {
                isFitToContents = true
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        fireDismissOnce()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        fireDismissOnce()
    }

    /**
     * iOS dispara `onDismiss` exatamente uma vez (em `presentationControllerDidDismiss`
     * no swipe, ou no `onCancelTap`). No Android, swipe/back chamam `onCancel` E
     * `onDismiss`, então o guard garante uma única invocação. O fluxo de "Concordar e
     * continuar" suprime o callback via [notifyDismiss] (iOS não chama `onDismiss` ali).
     */
    private fun fireDismissOnce() {
        if (didFireDismiss) return
        didFireDismiss = true
        if (notifyDismiss) onDismissed?.invoke()
    }

    private fun sheetBackgroundColor(): Int {
        return when (ThemeManager.colorScheme) {
            ColorScheme.BLACK -> Color.BLACK
            ColorScheme.DARK -> Color.rgb(28, 28, 30)
            ColorScheme.LIGHT -> DSSColors.background()
        }
    }

    /**
     * iOS: `preferredCornerRadius = 16` — arredonda somente os cantos superiores
     * (os inferiores ficam encostados na borda da tela). Usa a cor de fundo do
     * esquema atual (`setupSheet` do iOS).
     */
    private fun sheetBackground(): GradientDrawable {
        val ctx = requireContext()
        val r = 16f.dpToPxFloat(ctx)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(sheetBackgroundColor())
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
    }

    companion object {
        private const val TAG = "EsimInstallationSheet"
        private const val ARG_TEXT = "text"

        fun newInstance(text: String): EsimInstallationSheetFragment {
            return EsimInstallationSheetFragment().apply {
                arguments = Bundle().apply { putString(ARG_TEXT, text) }
            }
        }

        /**
         * Helper estático que dispara o sheet com os callbacks.
         */
        fun present(
            fragmentManager: FragmentManager,
            text: String,
            onDismiss: (() -> Unit)? = null,
            accepted: (() -> Unit)? = null,
        ) {
            val sheet = newInstance(text).apply {
                this.onDismissed = onDismiss
                this.accepted = accepted
            }
            sheet.show(fragmentManager, TAG)
        }
    }
}
