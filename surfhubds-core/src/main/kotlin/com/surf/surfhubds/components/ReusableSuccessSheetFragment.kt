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
import androidx.annotation.ColorInt
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
 * Port do `ReusableSuccessSheetViewController` do iOS — bottom sheet de sucesso reutilizável
 * com auto-dismiss configurável (default 5s).
 */
class ReusableSuccessSheetFragment : BottomSheetDialogFragment() {

    private var image: Drawable? = null
    private var text: String = ""
    @ColorInt private var textColor: Int? = null
    private var autoDismissMillis: Long = 5_000L
    var onDismissCallback: (() -> Unit)? = null

    private var didFireDismiss = false
    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable {
        if (isAdded) dismissAllowingStateLoss()
    }

    fun configure(
        image: Drawable?,
        text: String,
        @ColorInt textColor: Int? = null,
        autoDismissMillis: Long = 5_000L,
        onDismiss: (() -> Unit)? = null,
    ) {
        this.image = image
        this.text = text
        this.textColor = textColor
        this.autoDismissMillis = autoDismissMillis
        this.onDismissCallback = onDismiss
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        val ctx = requireContext()
        // iOS `setupSheet`: fundo varia por colorScheme (.black -> preto,
        // .dark -> rgb(28,28,30), default -> DSSColors.background) e
        // `preferredCornerRadius = 16` arredonda apenas os cantos superiores.
        val root = FrameLayout(ctx).apply {
            background = sheetBackground()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
            )
            minimumHeight = 360f.dpToPx(ctx)
        }

        val successView = BottomSheetSuccessView(ctx).apply {
            configure(image = this@ReusableSuccessSheetFragment.image,
                text = this@ReusableSuccessSheetFragment.text,
                textColor = this@ReusableSuccessSheetFragment.textColor)
        }
        root.addView(successView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
        ).apply { gravity = Gravity.CENTER })

        // iOS `setupSheet`: `prefersGrabberVisible = true` -> grabber/drag handle no topo.
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
            // iOS `setupSheet`: `preferredCornerRadius = 16` aplicado ao próprio sheet.
            sheet?.background = sheetBackground()
        }
        return dialog
    }

    /**
     * Fundo do sheet espelhando o `setupSheet` do iOS: cor por `colorScheme`
     * e cantos superiores arredondados em 16dp (`preferredCornerRadius = 16`).
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

    @ColorInt
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
            image: Drawable?,
            text: String,
            @ColorInt textColor: Int? = null,
            autoDismissMillis: Long = 5_000L,
            onDismiss: (() -> Unit)? = null,
        ): ReusableSuccessSheetFragment {
            val sheet = ReusableSuccessSheetFragment()
            sheet.configure(image, text, textColor, autoDismissMillis, onDismiss)
            sheet.show(activity.supportFragmentManager, "ReusableSuccessSheet")
            return sheet
        }
    }
}
