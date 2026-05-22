package com.surf.surfhubds.components

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import com.surf.surfhubds.font.DSSFont
import com.surf.surfhubds.theme.DSSColors
import com.surf.surfhubds.theme.Theme
import com.surf.surfhubds.theme.ThemeAware
import com.surf.surfhubds.theme.ThemeManager
import com.surf.surfhubds.theme.setupThemeObserver
import com.surf.surfhubds.tokens.ColorScheme
import com.surf.surfhubds.util.DrawableFactory
import com.surf.surfhubds.util.dpToPx

/**
 * Port do `DSSSelector` do iOS — segmented control programático com N opções.
 *
 * Cada opção é um botão pill; o selecionado fica colorido com a primary, os outros ficam
 * com background secundário. Suporta dark mode.
 */
class DSSSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), ThemeAware {

    interface Delegate {
        fun didSelect(selectedIndex: Int)
    }

    var delegate: Delegate? = null
    var onSelectionChanged: ((Int) -> Unit)? = null

    private val buttons = mutableListOf<AppCompatButton>()
    private var titles: List<String> = emptyList()
    private var images: List<Drawable?>? = null
    private var selectedIndex: Int = 0

    @ColorInt private var selectedTintColor: Int = Color.WHITE
    @ColorInt private var unselectedTintColor: Int = Color.BLACK
    @ColorInt private var selectedBackgroundColor: Int = DSSColors.primary()
    @ColorInt private var unselectedBackgroundColor: Int = Color.WHITE
    private var buttonCornerRadius: Float = 22f
    private var containerCornerRadius: Float = 12f

    private val stackView = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    init {
        addView(
            stackView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        applyThemeColors()
        refreshContainer()
        setupThemeObserver()
    }

    fun configure(
        titles: List<String>,
        images: List<Drawable?>? = null,
        initialSelection: Int = 0,
        @ColorInt selectedTintColor: Int = Color.WHITE,
        @ColorInt unselectedTintColor: Int = Color.BLACK,
        @ColorInt selectedBackgroundColor: Int = DSSColors.primary(),
        @ColorInt unselectedBackgroundColor: Int = Color.WHITE,
        cornerRadius: Float = 12f,
    ) {
        this.titles = titles
        this.images = images
        this.selectedIndex = initialSelection
        this.selectedTintColor = selectedTintColor
        this.unselectedTintColor = unselectedTintColor
        this.selectedBackgroundColor = selectedBackgroundColor
        this.unselectedBackgroundColor = unselectedBackgroundColor
        this.buttonCornerRadius = cornerRadius

        applyThemeColors()
        rebuild()
        updateSelection(initialSelection)
    }

    private fun rebuild() {
        stackView.removeAllViews()
        buttons.clear()

        for ((index, title) in titles.withIndex()) {
            val button = AppCompatButton(context).apply {
                this.text = title
                isAllCaps = false
                gravity = Gravity.CENTER
                textSize = 14f
                typeface = DSSFont.light(context, 14f).typeface
                tag = index
                setOnClickListener { updateSelection(index) }

                images?.getOrNull(index)?.let { drawable ->
                    setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                    compoundDrawablePadding = 6f.dpToPx(context)
                }
            }
            buttons.add(button)

            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                if (index > 0) leftMargin = 5f.dpToPx(context)
            }
            stackView.addView(button, lp)
        }
    }

    fun updateSelection(selectedIndex: Int) {
        this.selectedIndex = selectedIndex
        for ((index, button) in buttons.withIndex()) {
            val isMiddle = index in 1 until buttons.size - 1
            val cornerRadiusDp = if (isMiddle) 0f else buttonCornerRadius
            val isSelected = index == selectedIndex
            val tint = if (isSelected) selectedTintColor else unselectedTintColor
            val bg = if (isSelected) selectedBackgroundColor else unselectedBackgroundColor

            button.background = DrawableFactory.rounded(
                context = context,
                backgroundColor = bg,
                cornerRadiusDp = cornerRadiusDp,
            )
            button.setTextColor(tint)

            // Tint dos compound drawables (icone do lado do título)
            button.compoundDrawables.firstOrNull()?.let {
                it.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
            }
        }
        delegate?.didSelect(selectedIndex)
        onSelectionChanged?.invoke(selectedIndex)
    }

    private fun applyThemeColors() {
        val isDark = ThemeManager.colorScheme == ColorScheme.DARK ||
            ThemeManager.colorScheme == ColorScheme.BLACK
        if (isDark) {
            val selectorBackground = Color.rgb(64, 64, 64) // ~ white:0.25
            this.unselectedBackgroundColor = selectorBackground
            this.unselectedTintColor = Color.WHITE
            this.selectedTintColor = selectorBackground
            this.selectedBackgroundColor = Color.WHITE
        } else {
            this.unselectedBackgroundColor = Color.WHITE
            this.unselectedTintColor = Color.BLACK
            this.selectedTintColor = Color.WHITE
            this.selectedBackgroundColor = DSSColors.primary()
        }
    }

    private fun refreshContainer() {
        background = DrawableFactory.rounded(
            context = context,
            backgroundColor = unselectedBackgroundColor,
            cornerRadiusDp = containerCornerRadius,
        )
        clipToOutline = true
    }

    override fun applyTheme(theme: Theme) {
        applyThemeColors()
        refreshContainer()
        if (buttons.isNotEmpty()) {
            updateSelection(selectedIndex)
        }
    }
}
