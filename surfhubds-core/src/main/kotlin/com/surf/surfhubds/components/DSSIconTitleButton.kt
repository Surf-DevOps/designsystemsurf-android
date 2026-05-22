package com.surf.surfhubds.components

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.setMargins
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSIconTitleButton` do iOS — botão com ícone + título, seleção toggleable,
 * borda muda de cor quando selecionado.
 */
class DSSIconTitleButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    private val iconView = ImageView(context)
    private val titleView = TextView(context)
    private val stack = LinearLayout(context)

    var onTap: ((String) -> Unit)? = null
    var onSelectionChange: ((String) -> Unit)? = null

    /** Payload custom enviado nos callbacks. Cai pro title se for null. */
    var payloadString: String? = null

    @ColorInt private var normalBorderColor: Int = DSSColors.primary()

    private var selectedInternal: Boolean = false

    val titleText: CharSequence get() = titleView.text

    init {
        isClickable = true
        isFocusable = true
        minimumHeight = 50f.dpToPx(context)

        stack.orientation = LinearLayout.HORIZONTAL
        stack.gravity = Gravity.CENTER_VERTICAL
        stack.isClickable = false
        stack.isFocusable = false

        iconView.layoutParams = LinearLayout.LayoutParams(22.dp, 22.dp).apply {
            rightMargin = 8.dp
        }
        iconView.scaleType = ImageView.ScaleType.FIT_CENTER

        titleView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        titleView.textSize = 14f
        titleView.typeface = DSSFont.regular(context, 14f).typeface

        stack.addView(iconView)
        stack.addView(titleView)
        val stackParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        stackParams.setMargins(26.dp, 8.dp, 16.dp, 8.dp)
        stackParams.gravity = Gravity.CENTER_VERTICAL
        addView(stack, stackParams)

        setOnClickListener {
            isSelectedToggle = !isSelectedToggle
            val value = payloadString ?: titleView.text.toString()
            onTap?.invoke(value)
        }

        refresh()
        setupThemeObserver()
    }

    var isSelectedToggle: Boolean
        get() = selectedInternal
        set(value) {
            selectedInternal = value
            updateSelectionAppearance()
            val v = payloadString ?: titleView.text.toString()
            onSelectionChange?.invoke(v)
        }

    fun configure(
        title: CharSequence,
        icon: Drawable?,
        @ColorInt borderColor: Int = DSSColors.primary(),
        @ColorInt titleColor: Int = DSSColors.buttonText(),
    ) {
        titleView.text = title
        titleView.setTextColor(titleColor)
        iconView.setImageDrawable(icon)
        iconView.setColorFilter(titleColor, PorterDuff.Mode.SRC_IN)
        normalBorderColor = borderColor
        updateSelectionAppearance()
    }

    override fun applyTheme(theme: Theme) { refresh() }

    private fun refresh() = updateSelectionAppearance()

    private fun updateSelectionAppearance() {
        val color = if (selectedInternal) DSSColors.primary() else normalBorderColor
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = android.graphics.Color.TRANSPARENT,
            cornerRadiusDp = 25f,
            strokeColor = color,
            strokeWidthDp = 2f,
        )
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
