package com.surf.surfhubds.components

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme

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

    fun setContentText(text: String) { contentText = text }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        contentText = arguments?.getString(ARG_TEXT, "") ?: contentText

        val view = BottomSheetEsimInstallationView(ctx).apply {
            configure(contentText)
            setBackgroundColor(sheetBackgroundColor())
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
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet,
            )
            sheet?.setBackgroundColor(sheetBackgroundColor())
        }
        return dialog
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        if (notifyDismiss) onDismissed?.invoke()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (notifyDismiss) onDismissed?.invoke()
    }

    private fun sheetBackgroundColor(): Int {
        return when (ThemeManager.colorScheme) {
            ColorScheme.BLACK -> Color.BLACK
            ColorScheme.DARK -> Color.rgb(28, 28, 30)
            ColorScheme.LIGHT -> DSSColors.background()
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
