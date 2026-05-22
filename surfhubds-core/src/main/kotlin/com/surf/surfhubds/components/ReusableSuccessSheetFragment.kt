package com.surf.surfhubds.components

import android.graphics.drawable.Drawable
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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.util.dpToPx

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
        val root = FrameLayout(ctx).apply {
            setBackgroundColor(DSSColors.background())
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

        return root
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
