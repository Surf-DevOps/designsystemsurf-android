package com.surf.surfhubds.components

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.dpToPx
import com.surf.surfhubds.util.dpToPxFloat

/**
 * Port do welcome sheet de `HomeViewController.showWelcomeBottomSheet()` do iOS —
 * hospeda o [BottomSheetWelcomeView] num bottom sheet com auto-dismiss (default 5s),
 * espelhando o `present(welcomeVC)` + `asyncAfter(5) { dismiss }` do iOS.
 *
 * O `onDismiss` deve disparar o gate seguinte (no iOS: checkProfileCompleteness + limpar
 * a flag showActivationSuccessBottomSheet).
 */
class WelcomeSheetFragment : BottomSheetDialogFragment() {

    private var title: String = ""
    private var image: Drawable? = null
    private var msisdn: String = ""
    private var autoDismissMillis: Long = 5_000L
    var onDismissCallback: (() -> Unit)? = null

    private var didFireDismiss = false
    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { if (isAdded) dismissAllowingStateLoss() }

    fun configure(
        title: String,
        image: Drawable?,
        msisdn: String,
        autoDismissMillis: Long = 5_000L,
        onDismiss: (() -> Unit)? = null,
    ) {
        this.title = title
        this.image = image
        this.msisdn = msisdn
        this.autoDismissMillis = autoDismissMillis
        this.onDismissCallback = onDismiss
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        val root = FrameLayout(ctx).apply {
            background = sheetBackground()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
            )
            minimumHeight = 360f.dpToPx(ctx)
        }

        val welcomeView = BottomSheetWelcomeView(ctx).apply {
            configure(
                title = this@WelcomeSheetFragment.title,
                image = this@WelcomeSheetFragment.image,
                msisdn = this@WelcomeSheetFragment.msisdn,
            )
        }
        root.addView(welcomeView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ).apply { gravity = Gravity.CENTER })

        val grabber = BottomSheetDragHandleView(ctx)
        root.addView(grabber, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL,
        ))
        return root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet,
            )
            sheet?.background = sheetBackground()
        }
        return dialog
    }

    private fun sheetBackground(): GradientDrawable {
        val ctx = requireContext()
        val r = 16f.dpToPxFloat(ctx)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(sheetBackgroundColor())
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
    }

    private fun sheetBackgroundColor(): Int = when (ThemeManager.colorScheme) {
        ColorScheme.BLACK -> Color.BLACK
        ColorScheme.DARK -> Color.rgb(28, 28, 30)
        ColorScheme.LIGHT -> DSSColors.background()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(dismissRunnable)
        handler.postDelayed(dismissRunnable, autoDismissMillis)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(dismissRunnable)
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (!didFireDismiss) {
            didFireDismiss = true
            handler.removeCallbacks(dismissRunnable)
            onDismissCallback?.invoke()
        }
    }

    companion object {
        fun present(
            activity: FragmentActivity,
            title: String,
            image: Drawable?,
            msisdn: String,
            autoDismissMillis: Long = 5_000L,
            onDismiss: (() -> Unit)? = null,
        ): WelcomeSheetFragment {
            val sheet = WelcomeSheetFragment()
            sheet.configure(title, image, msisdn, autoDismissMillis, onDismiss)
            sheet.show(activity.supportFragmentManager, "WelcomeSheet")
            return sheet
        }
    }
}
